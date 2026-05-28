CREATE TABLE IF NOT EXISTS message_outbox (
    id BIGINT PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    payload_json TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=PENDING,1=SENDING,2=SENT,3=FAILED',
    retry_count INT NOT NULL DEFAULT 0,
    max_retry_count INT NOT NULL DEFAULT 20,
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_outbox_status_retry (status, next_retry_at),
    KEY idx_outbox_aggregate (aggregate_type, aggregate_id)
);
