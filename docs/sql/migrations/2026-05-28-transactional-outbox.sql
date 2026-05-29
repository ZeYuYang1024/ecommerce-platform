CREATE TABLE IF NOT EXISTS message_outbox (
    id BIGINT PRIMARY KEY COMMENT '雪花ID',
    aggregate_type VARCHAR(64) NOT NULL COMMENT '聚合类型，如 order/payment/inventory',
    aggregate_id VARCHAR(64) NOT NULL COMMENT '聚合业务ID，如订单号或支付单号',
    topic VARCHAR(128) NOT NULL COMMENT 'MQ topic',
    payload_json TEXT NOT NULL COMMENT '消息载荷 JSON',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '发送状态：0=PENDING,1=SENDING,2=SENT,3=FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry_count INT NOT NULL DEFAULT 20 COMMENT '最大重试次数',
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次可重试时间',
    last_error VARCHAR(512) NULL COMMENT '最近一次发送失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_outbox_status_retry (status, next_retry_at) COMMENT '按状态和重试时间扫描待发送消息',
    KEY idx_outbox_aggregate (aggregate_type, aggregate_id) COMMENT '按聚合维度查询消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事务消息外发表';
