#!/usr/bin/env bash
# Start a local demo and keep the application processes attached to this script.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
root="$PWD"

shop_only=false
skip_java_build=false
for option in "$@"; do
  case "$option" in
    --shop-only) shop_only=true ;;
    --skip-java-build) skip_java_build=true ;;
    *) echo "Usage: bash scripts/start-demo.sh [--shop-only] [--skip-java-build]" >&2; exit 2 ;;
  esac
done

offset=${DEMO_PORT_OFFSET:-0}
mysql_port=${DEMO_MYSQL_PORT:-3306}
redis_port=${DEMO_REDIS_PORT:-6379}
nacos_port=${DEMO_NACOS_PORT:-8848}
rocket_port=${DEMO_ROCKETMQ_PORT:-9876}
minio_port=${DEMO_MINIO_PORT:-9000}
search_port=${DEMO_ELASTICSEARCH_PORT:-9200}
web_port=$((3000 + offset))
admin_port=$((5173 + offset))
export DEMO_GATEWAY_URL="http://127.0.0.1:$((8080 + offset))"
export DEMO_ELASTICSEARCH_URL="http://127.0.0.1:$search_port"
export DEV_GATEWAY_URL="$DEMO_GATEWAY_URL"
logs="${DEMO_LOG_DIR:-$root/.build-local/demo}"
mkdir -p "$logs"

middleware=(mysql redis nacos rocketmq-namesrv rocketmq-broker minio)
modules=(auth:8091 user:8081 product:8082 inventory:8083 cart:8086 order:8084 payment:8085
         file:8090 member:8096 recommendation:8099)
if ! "$shop_only"; then
  middleware+=(elasticsearch)
  modules+=(merchant:8087 coupon:8088 notification:8089 search:8092 seckill:8093 monitor:8094)
fi
modules+=(gateway:8080)

ports=("$web_port")
if ! "$shop_only"; then ports+=("$admin_port"); fi
for entry in "${modules[@]}"; do
  port=${entry#*:}
  ports+=("$((port + offset))")
done
python3 - "${ports[@]}" <<'PY'
import socket
import sys

for port in map(int, sys.argv[1:]):
    with socket.socket() as sock:
        if sock.connect_ex(("127.0.0.1", port)) == 0:
            sys.exit(f"Application port {port} is already in use. Stop the existing app before starting the demo.")
PY

pids=()
cleanup() {
  if [[ -n ${pids[0]:-} ]]; then
    kill "${pids[@]}" 2>/dev/null || true
    wait "${pids[@]}" 2>/dev/null || true
  fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

await_ready() {
  local label=$1
  shift
  local deadline=$((SECONDS + 180))
  until "$@" >/dev/null 2>&1; do
    if (( SECONDS >= deadline )); then
      echo "Timed out waiting for $label. Check Docker logs or $logs." >&2
      return 1
    fi
    sleep 2
  done
}

http() { curl --noproxy '*' -fsS --max-time 5 "$1"; }
db() {
  docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$@"' mysql "$@"
}
broker_ready() {
  docker compose exec -T rocketmq-broker sh mqadmin clusterList -n rocketmq-namesrv:9876 | grep 'DefaultCluster'
}
nacos_ready() {
  http "http://127.0.0.1:$nacos_port/nacos/v3/admin/core/state/readiness" | grep '"data":"ok"'
}
redis_ready() {
  [[ $(docker compose exec -T redis redis-cli --no-auth-warning -a root ping) == PONG ]]
}
gateway_ready() {
  http "$DEMO_GATEWAY_URL/api/v1/products/1" | python3 -c 'import json,sys; r=json.load(sys.stdin); sys.exit(r.get("code") != 200 or not r["data"]["skus"])'
}
start_app() {
  local name=$1
  local directory=$2
  shift 2
  (cd "$directory"; exec "$@") >"$logs/$name.log" 2>&1 &
  pids+=("$!")
}

docker compose up -d "${middleware[@]}"
await_ready MySQL db -Nse 'SELECT 1'
await_ready Redis redis_ready
await_ready Nacos nacos_ready
await_ready RocketMQ broker_ready
await_ready MinIO http "http://127.0.0.1:$minio_port/minio/health/live"
if ! "$shop_only"; then
  await_ready Elasticsearch http "$DEMO_ELASTICSEARCH_URL/_cluster/health"
fi

migrations=docs/sql/migrations
db < docs/init.sql
db < ecommerce-recommendation/sql/init.sql
db < "$migrations/2026-05-17-merchant-marketing-tenant-columns.sql"
db ecommerce_inventory < "$migrations/2026-05-28-inventory-event-log.sql"
db < "$migrations/2026-06-01-member-init.sql"
db < "$migrations/2026-06-02-member-phase2a-init.sql"
db < "$migrations/2026-06-02-order-points-reservation.sql"
for schema in ecommerce_inventory ecommerce_order ecommerce_payment; do
  db "$schema" < "$migrations/2026-05-28-transactional-outbox.sql"
done
if [[ $(db -Nse "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='ecommerce_payment' AND TABLE_NAME='payment' AND INDEX_NAME='uk_payment_order_no'") == 0 ]]; then
  db ecommerce_payment < "$migrations/2026-05-29-payment-order-unique.sql"
fi

for topic in order-created order-cancelled order-paid order-refunded product-created merchant-approved review-created seckill-order outbound-shipped shipping-dispatched shipping-exception order-delivered warehouse-stock-changed recommendation-signal; do
  result=$(docker compose exec -T rocketmq-broker sh mqadmin updateTopic     -n rocketmq-namesrv:9876 -c DefaultCluster -t "$topic" -r 4 -w 4 2>&1)
  if [[ $result != *success* ]]; then printf '%s\n' "$result" >&2; exit 1; fi
done

if ! "$skip_java_build"; then mvn -B -ntp -Dmaven.test.skip=true package; fi
for entry in "${modules[@]}"; do
  module=${entry%:*}
  port=${entry#*:}
  name="ecommerce-$module"
  jar="$root/$name/target/$name-1.0-SNAPSHOT.jar"
  if [[ ! -f "$jar" ]]; then echo "Missing $jar" >&2; exit 1; fi
  args=(--server.address=127.0.0.1 "--server.port=$((port + offset))"
        --spring.cloud.nacos.discovery.ip=127.0.0.1
        "--spring.cloud.nacos.discovery.server-addr=127.0.0.1:$nacos_port"
        --spring.boot.admin.client.enabled=false
        --spring.data.redis.host=127.0.0.1 "--spring.data.redis.port=$redis_port"
        --spring.data.redis.password=root "--rocketmq.name-server=127.0.0.1:$rocket_port")
  # Only modules with a datasource receive the matching local database URL.
  case "$module" in
    gateway|cart|file|search|monitor) ;;
    recommendation)
      args+=("--spring.datasource.url=jdbc:mysql://127.0.0.1:$mysql_port/ecommerce_recommendation?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true") ;;
    *) args+=("--spring.datasource.url=jdbc:mysql://127.0.0.1:$mysql_port/ecommerce_$module?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai") ;;
  esac
  if [[ $module == file ]]; then args+=("--minio.endpoint=http://127.0.0.1:$minio_port"); fi
  if [[ $module == search ]]; then args+=("--elasticsearch.host=127.0.0.1" "--elasticsearch.port=$search_port"); fi
  start_app "$name" "$root/$name" java -XX:ActiveProcessorCount=2 -Xms64m -Xmx256m -jar "$jar" "${args[@]}"
done

for entry in "${modules[@]}"; do
  port=${entry#*:}
  await_ready "ecommerce-${entry%:*}" http "http://127.0.0.1:$((port + offset))/actuator/health"
done
await_ready 'gateway product route' gateway_ready
python3 scripts/seed-demo-product-images.py

npm --prefix ecommerce-web ci
start_app storefront "$root/ecommerce-web" ./node_modules/.bin/nuxt dev --host 127.0.0.1 --port "$web_port"
await_ready storefront http "http://127.0.0.1:$web_port"
printf 'Storefront %s (testuser / test123)\n' "http://127.0.0.1:$web_port"

if ! "$shop_only"; then
  npm --prefix ecommerce-admin ci
  start_app admin "$root/ecommerce-admin" ./node_modules/.bin/vite --host 127.0.0.1 --port "$admin_port" --strictPort
  await_ready admin http "http://127.0.0.1:$admin_port"
  printf 'Admin %s (admin / admin123)\n' "http://127.0.0.1:$admin_port"
fi
printf 'Demo ready. Logs: %s\n' "$logs"
# Wait for application failures on both macOS Bash and newer Bash releases.
while true; do
  for pid in "${pids[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "A demo process stopped. Check $logs." >&2
      exit 1
    fi
  done
  sleep 2
done
