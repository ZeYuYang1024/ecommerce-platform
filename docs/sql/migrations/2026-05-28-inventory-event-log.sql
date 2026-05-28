CREATE TABLE IF NOT EXISTS inventory_event_log (
    id BIGINT PRIMARY KEY,
    topic VARCHAR(128) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=PROCESSING,1=PROCESSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inventory_event_topic_order (topic, order_no),
    KEY idx_inventory_event_status (status)
);
