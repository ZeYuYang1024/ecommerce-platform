#!/bin/bash
# ============================================================
# E-Commerce Platform — 中间件启动脚本 (macOS / Linux)
# 用法: bash scripts/start-middleware.sh
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================"
echo "  E-Commerce 中间件启动"
echo "========================================"

# --------------- Docker Compose ---------------
echo ""
echo "[1/4] 启动 Docker 容器..."
cd "$PROJECT_DIR"
docker compose up -d
echo "容器已启动。"

# --------------- 等待就绪 ---------------
echo ""
echo "[2/4] 等待服务就绪..."

echo -n "  MySQL ... "
until docker exec ecommerce-mysql mysqladmin ping -uroot -proot --silent 2>/dev/null; do sleep 2; done
echo "OK"

echo -n "  Redis ... "
until docker exec ecommerce-redis redis-cli -a root ping 2>/dev/null | grep -q PONG; do sleep 1; done
echo "OK"

echo -n "  Nacos ... "
until curl -s http://localhost:8848/nacos/v1/console/health/readiness 2>/dev/null | grep -q "ok"; do sleep 3; done
echo "OK"

echo -n "  RocketMQ NameServer ... "
until docker logs ecommerce-rmq-namesrv 2>/dev/null | grep -q "Started"; do sleep 3; done
echo "OK"

echo -n "  RocketMQ Broker ... "
until docker exec ecommerce-rmq-broker ./mqadmin clusterList -n rocketmq-namesrv:9876 2>/dev/null | grep -q "BID"; do sleep 3; done
echo "OK"

echo -n "  Elasticsearch ... "
until curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -q '"status"'; do sleep 3; done
echo "OK"

echo -n "  MinIO ... "
until curl -s http://localhost:9000/minio/health/live 2>/dev/null | grep -q "OK"; do sleep 2; done
echo "OK"

# --------------- RocketMQ Topics ---------------
echo ""
echo "[3/4] 初始化 RocketMQ Topics ..."
BROKER="ecommerce-rmq-broker"
NS="rocketmq-namesrv:9876"

TOPICS=(
    "order-created:4"
    "order-cancelled:4"
    "order-paid:4"
    "product-created:4"
    "merchant-approved:4"
)

for entry in "${TOPICS[@]}"; do
    TOPIC="${entry%%:*}"
    QUEUES="${entry##*:}"
    echo -n "  Creating $TOPIC (queues=$QUEUES)... "
    docker exec "$BROKER" ./mqadmin updateTopic \
        -n "$NS" -c DefaultCluster -t "$TOPIC" \
        -r "$QUEUES" -w "$QUEUES" 2>&1 | grep -q "success" && echo "OK" || echo "SKIP (may exist)"
done

# --------------- Init SQL ---------------
echo ""
echo "[4/4] 初始化数据库 ..."
docker exec -i ecommerce-mysql mysql -uroot -proot < "$PROJECT_DIR/docs/init.sql"
echo "数据库初始化完成。"

echo ""
echo "========================================"
echo "  中间件全部就绪！"
echo ""
echo "  访问地址:"
echo "    Nacos:    http://localhost:8848/nacos"
echo "    MinIO:    http://localhost:9001"
echo "    ES:       http://localhost:9200"
echo "    Druid:    http://localhost:{服务端口}/druid/sql.html"
echo "    SBA:      http://localhost:8094/admin"
echo ""
echo "  启动后端:   cd <module> && mvn spring-boot:run"
echo "========================================"
