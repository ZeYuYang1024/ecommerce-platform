"""Upload catalog illustrations and fill missing images on the local SQL seed products."""

import json
import os
from pathlib import Path
import re
import subprocess
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener
from uuid import uuid4


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "docs" / "demo-product-images"
GATEWAY = os.environ.get("DEMO_GATEWAY_URL", "http://127.0.0.1:8080").rstrip("/")
ELASTICSEARCH = os.environ.get("DEMO_ELASTICSEARCH_URL", "http://127.0.0.1:9200").rstrip("/")
HTTP = build_opener(ProxyHandler({}))
PRODUCTS = {
    "01-refrigerator": (1, "海尔双开门冰箱 BCD-500"),
    "11-mattress": (11, "宜家独立弹簧床垫 1.8m"),
    "12-jacket": (12, "商务休闲立领夹克"),
    "13-floral-dress": (13, "法式复古碎花连衣裙"),
    "14-running-shoe-black-white": (14, "耐克 Air Zoom Pegasus 42"),
    "15-running-shoe-white": (15, "阿迪达斯 Ultraboost 5.0 女款"),
    "16-sunscreen": (16, "欧莱雅小金管防晒霜 SPF50+"),
    "17-lip-glaze": (17, "欧莱雅小钢笔唇釉"),
    "18-mixed-nuts": (18, "三只松鼠坚果大礼包 1688g"),
    "19-watch": (19, "华为 WATCH GT 5 Pro"),
    "20-keyboard": (20, "罗技 MX Keys S 无线键盘"),
}


def call(url, method="GET", data=None, headers=None):
    with HTTP.open(Request(url, data=data, headers=headers or {}, method=method), timeout=15) as response:
        return json.load(response)


def api(path, method="GET", data=None, headers=None):
    result = call(GATEWAY + "/api/v1" + path, method, data, headers)
    if result.get("code") != 200:
        raise RuntimeError(f"{method} {path}: {result.get('message') or result.get('code')}")
    return result.get("data")


def log_in():
    credentials = json.dumps({
        "username": os.environ.get("DEMO_GATEWAY_USERNAME", "testuser"),
        "password": os.environ.get("DEMO_GATEWAY_PASSWORD", "test123"),
    }).encode()
    result = api("/auth/login", "POST", credentials, {"Content-Type": "application/json"})
    return {"Authorization": "Bearer " + result["token"]}


def upload(path, authorization):
    boundary = uuid4().hex
    prefix = (
        f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
        "Content-Type: image/jpeg\r\n\r\n"
    ).encode()
    data = prefix + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    return api("/files/upload", "POST", data, {
        **authorization, "Content-Type": f"multipart/form-data; boundary={boundary}",
    })


def sql(command):
    return subprocess.run(
        ["docker", "compose", "exec", "-T", "mysql", "sh", "-c",
         'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 --batch --raw --skip-column-names ecommerce_product'],
        cwd=ROOT, check=True, input=command, encoding="utf-8", stdout=subprocess.PIPE,
    ).stdout.splitlines()


def literal(value):
    return f"CONVERT(0x{value.encode('utf-8').hex()} USING utf8mb4)"


def sync_existing_search_images(images):
    if not images:
        return
    try:
        with HTTP.open(Request(ELASTICSEARCH + "/products", method="HEAD"), timeout=4):
            pass
    except HTTPError as error:
        if error.code != 404:
            raise
        print("No products search index yet. Follow the demo search indexing step in the README.")
        return
    except URLError:
        print("Elasticsearch is unavailable. Rerun this script to sync indexed demo products.")
        return

    lines = []
    source = (
        "if (ctx._source.name == params.name && ctx._source.mainImage != params.image) "
        "{ ctx._source.mainImage = params.image } else { ctx.op = 'noop' }"
    )
    for product_id, name, image in images:
        lines.append(json.dumps({"update": {"_index": "products", "_id": str(product_id)}}))
        lines.append(json.dumps({"script": {"source": source, "params": {"name": name, "image": image}}}))
    response = call(ELASTICSEARCH + "/_bulk?refresh=true", "POST", ("\n".join(lines) + "\n").encode(),
                    {"Content-Type": "application/x-ndjson"})
    updates = [row["update"] for row in response["items"]]
    failed = [row for row in updates if row.get("status", 500) >= 400 and row.get("status") != 404]
    if failed:
        raise RuntimeError(f"Elasticsearch updates failed: {failed}")
    print(f"Refreshed {sum(row.get('result') == 'updated' for row in updates)} existing search images.")


def main():
    source = ",".join(str(product_id) for product_id, _ in PRODUCTS.values())
    records = {
        row[0]: row[1:]
        for row in map(json.loads, sql(f"SELECT JSON_ARRAY(id,name,main_image) FROM spu WHERE id IN ({source});"))
    }
    seen = []
    authorization = None
    uploaded = 0
    for filename, (product_id, name) in PRODUCTS.items():
        record = records.get(product_id)
        if record is None or record[0] != name:
            continue
        path = ASSETS / (filename + ".jpg")
        image = record[1]
        condition = f"id = {product_id} AND name = {literal(name)}"
        if not image or not image.strip():
            if authorization is None:
                authorization = log_in()
            uploaded_image = upload(path, authorization)
            if not re.fullmatch(r"[0-9]+\.jpg", uploaded_image or ""):
                raise RuntimeError(f"Unexpected image reference for product {product_id}")
            updated = sql(
                f"UPDATE spu SET main_image={literal(uploaded_image)} "
                f"WHERE {condition} AND COALESCE(TRIM(main_image),'')=''; "
                f"SELECT JSON_ARRAY(main_image) FROM spu WHERE {condition};"
            )
            image = json.loads(updated[0])[0] if updated else None
            if image == uploaded_image:
                uploaded += 1
            else:
                api(f"/files/{uploaded_image}", "DELETE", headers=authorization)
        if image and image.strip():
            sql(
                "UPDATE sku s JOIN spu p ON p.id=s.spu_id SET s.image=p.main_image "
                f"WHERE p.id={product_id} AND p.name={literal(name)} AND COALESCE(TRIM(s.image),'')='';"
            )
            seen.append((product_id, name, image))
    print(f"Uploaded {uploaded} demo images. {len(seen)} sample products have an image.")
    sync_existing_search_images(seen)


if __name__ == "__main__":
    main()
