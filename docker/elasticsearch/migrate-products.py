#!/usr/bin/env python3
"""Copy the products index into a fresh Elasticsearch instance."""

import argparse
import json
from urllib.request import ProxyHandler, Request, build_opener


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", help="Source Elasticsearch URL")
    parser.add_argument("destination", help="Destination Elasticsearch URL")
    args = parser.parse_args()
    source = args.source.rstrip("/")
    destination = args.destination.rstrip("/")
    opener = build_opener(ProxyHandler({}))

    def request(base, method, path, data=None):
        content_type = "application/json"
        if isinstance(data, str):
            content_type = "application/x-ndjson"
            data = data.encode()
        elif data is not None:
            data = json.dumps(data).encode()
        with opener.open(Request(base + path, data, {"Content-Type": content_type}, method=method),
                         timeout=40) as response:
            return json.load(response)

    expected = request(source, "GET", "/products/_count")["count"]
    mappings = request(source, "GET", "/products/_mapping")["products"]["mappings"]
    settings = request(source, "GET", "/products/_settings")["products"]["settings"]["index"]
    # Copy user-defined analysis, without the old index version or cluster metadata.
    index_settings = {key: settings[key] for key in ("analysis", "number_of_shards", "number_of_replicas")
                      if key in settings}
    request(destination, "PUT", "/products", {"mappings": mappings, "settings": index_settings})

    page = request(source, "POST", "/products/_search?scroll=2m", {"size": 500, "sort": ["_doc"]})
    scroll_id = page["_scroll_id"]
    try:
        while hits := page["hits"]["hits"]:
            rows = []
            for hit in hits:
                rows.append(json.dumps({"index": {"_index": "products", "_id": hit["_id"]}}))
                rows.append(json.dumps(hit["_source"]))
            result = request(destination, "POST", "/_bulk", "\n".join(rows) + "\n")
            if result["errors"]:
                raise RuntimeError("One or more products failed to index")
            page = request(source, "POST", "/_search/scroll", {"scroll": "2m", "scroll_id": scroll_id})
            scroll_id = page["_scroll_id"]
    finally:
        request(source, "DELETE", "/_search/scroll", {"scroll_id": [scroll_id]})

    request(destination, "POST", "/products/_refresh")
    actual = request(destination, "GET", "/products/_count")["count"]
    if actual != expected:
        raise RuntimeError(f"Source has {expected} products, destination has {actual}")
    print(f"Migrated {actual} products")


if __name__ == "__main__":
    main()
