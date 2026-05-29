CREATE TABLE IF NOT EXISTS inventory_event_log (
    id BIGINT PRIMARY KEY COMMENT '雪花ID',
    topic VARCHAR(128) NOT NULL COMMENT '消费主题，如 order-created/order-cancelled',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0=PROCESSING,1=PROCESSED,2=FAILED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0=未删除，1=已删除',
    UNIQUE KEY uk_inventory_event_topic_order (topic, order_no) COMMENT '同一主题同一订单只处理一次',
    KEY idx_inventory_event_status (status) COMMENT '按处理状态查询事件'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存事件处理日志表';
