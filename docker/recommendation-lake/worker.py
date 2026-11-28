import argparse
from collections import defaultdict
from contextlib import closing
from datetime import datetime, timedelta, timezone
import logging
import os
from pathlib import Path
import re
from tempfile import TemporaryDirectory
import time
from urllib.parse import urlsplit
from uuid import uuid4

import boto3
from botocore.config import Config
from botocore.exceptions import ClientError
import duckdb
import mysql.connector


LOG = logging.getLogger("recommendation-lake")
LOCK = "ecommerce-recommendation-lake"
BATCH_SIZE = 5000
MAX_BATCHES_PER_RUN = 20
EVENT_COLUMNS = """
    event_id VARCHAR, user_id BIGINT, spu_id BIGINT, category_id BIGINT, brand_id BIGINT,
    price DECIMAL(12, 2), event_type VARCHAR, request_id VARCHAR, surface VARCHAR, strategy VARCHAR,
    occurred_at TIMESTAMP
"""
RESULTS = (
    ("recommendation_user_category", "user_category", "user_id, category_id, score_7d, score_30d"),
    ("recommendation_related_product", "related_product", "spu_id, related_spu_id, shared_users, affinity"),
    ("recommendation_daily_report", "daily_report", "event_date, impressions, clicks, click_rate"),
    ("recommendation_funnel_report", "funnel_report", "event_date, surface, strategy, impressions, clicks, cart_adds, payments"),
)


def mysql_connection():
    return mysql.connector.connect(
        host=os.environ["MYSQL_HOST"],
        port=int(os.environ.get("MYSQL_PORT", "3306")),
        user=os.environ["MYSQL_USER"],
        password=os.environ["MYSQL_PASSWORD"],
        database="ecommerce_recommendation",
        autocommit=False,
        connection_timeout=5,
        use_pure=True,
    )


def object_store():
    endpoint = os.environ["S3_ENDPOINT"]
    access_key = os.environ["S3_ACCESS_KEY"]
    secret_key = os.environ["S3_SECRET_KEY"]
    s3 = boto3.client(
        "s3", endpoint_url=endpoint, aws_access_key_id=access_key, aws_secret_access_key=secret_key,
        region_name="us-east-1",
        config=Config(s3={"addressing_style": "path"}, request_checksum_calculation="when_required",
                      response_checksum_validation="when_required", connect_timeout=5, read_timeout=30),
    )
    return s3, endpoint, access_key, secret_key


def open_analytics(db, endpoint, access_key, secret_key):
    address = urlsplit(endpoint)
    db.execute("LOAD httpfs")
    db.execute("SET TimeZone = 'UTC'")
    db.execute("SET threads = 2")
    db.execute("SET memory_limit = '512MB'")
    db.execute("""
        CREATE SECRET recommendation_lake (
            TYPE s3, KEY_ID ?, SECRET ?, REGION 'us-east-1', ENDPOINT ?, URL_STYLE 'path', USE_SSL ?
        )
    """, [access_key, secret_key, address.netloc, address.scheme == "https"])
    db.execute(f"CREATE TEMP TABLE batch_events ({EVENT_COLUMNS})")


def literal(value):
    return "'" + value.replace("'", "''") + "'"


def ensure_bucket(s3, bucket):
    try:
        s3.head_bucket(Bucket=bucket)
    except ClientError as error:
        if error.response["ResponseMetadata"]["HTTPStatusCode"] != 404:
            raise
        s3.create_bucket(Bucket=bucket)


def claim(db):
    with db.cursor() as cursor:
        cursor.execute("""
            SELECT lake_batch_id FROM recommendation_event
            WHERE exported_at IS NULL AND lake_batch_id IS NOT NULL
            ORDER BY lake_batch_id LIMIT 1
        """)
        row = cursor.fetchone()
        if row:
            return row[0]

        cursor.execute("""
            SELECT id FROM recommendation_event
            WHERE exported_at IS NULL AND lake_batch_id IS NULL ORDER BY id LIMIT %s
        """, (BATCH_SIZE,))
        ids = [row[0] for row in cursor.fetchall()]
        if not ids:
            db.commit()
            return None
        batch_id = uuid4().hex
        markers = ", ".join(["%s"] * len(ids))
        cursor.execute(f"UPDATE recommendation_event SET lake_batch_id = %s WHERE id IN ({markers})",
                       [batch_id, *ids])
    db.commit()
    return batch_id


def export_batch(db, analytics, s3, bucket, batch_id):
    with db.cursor() as cursor:
        cursor.execute("""
            SELECT event_id, user_id, spu_id, category_id, brand_id, price, event_type, request_id, surface, strategy, occurred_at
            FROM recommendation_event WHERE exported_at IS NULL AND lake_batch_id = %s ORDER BY id
        """, (batch_id,))
        by_date = defaultdict(list)
        for row in cursor:
            by_date[row[-1].date()].append(row)
    db.commit()

    with TemporaryDirectory(prefix="recommendation-") as directory:
        for date, rows in by_date.items():
            analytics.execute("DELETE FROM batch_events")
            analytics.executemany("INSERT INTO batch_events VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", rows)
            path = Path(directory) / f"{date}.parquet"
            analytics.execute(f"COPY batch_events TO {literal(str(path))} (FORMAT parquet, COMPRESSION zstd)")
            key = f"events/event_date={date}/{batch_id}.parquet"
            # The same batch always writes the same object keys, including after a partial upload.
            with path.open("rb") as body:
                s3.put_object(Bucket=bucket, Key=key, Body=body, ContentType="application/vnd.apache.parquet")
    with db.cursor() as cursor:
        cursor.execute("""
            UPDATE recommendation_event SET exported_at = UTC_TIMESTAMP(3)
            WHERE exported_at IS NULL AND lake_batch_id = %s
        """, (batch_id,))
    db.commit()
    return sum(map(len, by_date.values()))


def publish(db, analytics, s3, bucket):
    today = datetime.now(timezone.utc).date()
    # One additional date covers an impression followed by a click after midnight.
    start = today - timedelta(days=30)
    if s3.list_objects_v2(Bucket=bucket, Prefix="events/", MaxKeys=1).get("KeyCount", 0):
        analytics.execute(f"""
            CREATE TEMP TABLE lake_events AS
            SELECT * FROM (
                SELECT * EXCLUDE (event_date)
                FROM read_parquet({literal(f"s3://{bucket}/events/event_date=*/*.parquet")},
                                  hive_partitioning = true, union_by_name = true)
                WHERE event_date BETWEEN DATE '{start}' AND DATE '{today}'
                UNION ALL BY NAME
                SELECT * FROM batch_events WHERE FALSE
            ) events
            QUALIFY row_number() OVER (PARTITION BY event_id ORDER BY occurred_at DESC) = 1
        """)
    else:
        analytics.execute(f"CREATE TEMP TABLE lake_events ({EVENT_COLUMNS})")

    analytics.execute(Path(__file__).with_name("analysis.sql").read_text())
    with db.cursor() as cursor:
        for destination, source, columns in RESULTS:
            cursor.execute(f"DELETE FROM {destination}")
            data = analytics.execute(f"SELECT {columns} FROM {source}")
            markers = ", ".join(["%s"] * len(columns.split(",")))
            while rows := data.fetchmany(1000):
                cursor.executemany(f"INSERT INTO {destination} ({columns}) VALUES ({markers})", rows)
    db.commit()


def run():
    bucket = os.environ.get("S3_BUCKET", "ecommerce-recommendation-lake")
    if not re.fullmatch(r"[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]", bucket):
        raise ValueError("Invalid S3_BUCKET")
    with closing(mysql_connection()) as db, closing(duckdb.connect()) as analytics:
        with db.cursor() as cursor:
            cursor.execute("SELECT GET_LOCK(%s, 0)", (LOCK,))
            if cursor.fetchone()[0] != 1:
                LOG.info("Another recommendation lake job is running")
                return
        s3, endpoint, access_key, secret_key = object_store()
        ensure_bucket(s3, bucket)
        open_analytics(analytics, endpoint, access_key, secret_key)

        exported = 0
        for _ in range(MAX_BATCHES_PER_RUN):
            batch_id = claim(db)
            if batch_id is None:
                break
            exported += export_batch(db, analytics, s3, bucket, batch_id)
        publish(db, analytics, s3, bucket)
        LOG.info("Exported %s events and published recommendation analytics", exported)


def main():
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    parser = argparse.ArgumentParser()
    parser.add_argument("--once", action="store_true", help="Run one export and analysis cycle")
    args = parser.parse_args()
    if args.once:
        run()
        return
    interval = max(30, int(os.environ.get("LAKE_INTERVAL_SECONDS", "900")))
    while True:
        try:
            run()
        except Exception:
            LOG.exception("Recommendation lake cycle failed")
        time.sleep(interval)


if __name__ == "__main__":
    main()
