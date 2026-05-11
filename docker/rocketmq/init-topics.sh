#!/bin/bash
# RocketMQ Topic 初始化脚本
# 在 docker-compose up -d rocketmq-broker 之后执行
# 用法: bash docker/rocketmq/init-topics.sh

BROKER_CONTAINER="ecommerce-rmq-broker"
NAMESRV="rocketmq-namesrv:9876"

# 等待 broker 就绪
echo "Waiting for RocketMQ broker to be ready..."
until docker exec $BROKER_CONTAINER ./mqadmin clusterList -n $NAMESRV 2>/dev/null | grep -q "BID"; do
    sleep 2
done
echo "Broker is ready."

# 定义所有 topic（topic名称 队列数）
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
    echo -n "Creating topic $TOPIC (queues=$QUEUES)... "
    docker exec $BROKER_CONTAINER ./mqadmin updateTopic \
        -n $NAMESRV \
        -c DefaultCluster \
        -t "$TOPIC" \
        -r "$QUEUES" \
        -w "$QUEUES" 2>&1 | grep -q "success" && echo "OK" || echo "FAILED"
done

echo "All topics initialized."
