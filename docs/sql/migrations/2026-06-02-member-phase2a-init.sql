USE ecommerce_member;

CREATE TABLE IF NOT EXISTS points_reservation (
    id BIGINT PRIMARY KEY,
    reservation_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    scene_type VARCHAR(32) NOT NULL,
    reserved_points INT NOT NULL,
    consumed_points INT NOT NULL DEFAULT 0,
    released_points INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(96) NOT NULL,
    expired_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (reservation_no),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    UNIQUE KEY uk_order_scene (order_no, scene_type),
    KEY idx_user_status (user_id, status),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='points reservation';

CREATE TABLE IF NOT EXISTS points_consume_detail (
    id BIGINT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    earn_tx_id BIGINT NOT NULL,
    consume_points INT NOT NULL,
    restored_points INT NOT NULL DEFAULT 0,
    expire_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_reservation_id (reservation_id),
    KEY idx_user_expire_at (user_id, expire_at),
    KEY idx_earn_tx_id (earn_tx_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='points consume detail';

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'points_transaction'
      AND COLUMN_NAME = 'related_reservation_no'
  ),
  'SELECT ''points_transaction.related_reservation_no already exists'' AS message',
  'ALTER TABLE points_transaction ADD COLUMN related_reservation_no VARCHAR(32) NULL COMMENT ''related reservation number'' AFTER remark'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'points_transaction'
      AND COLUMN_NAME = 'reversal_of_tx_id'
  ),
  'SELECT ''points_transaction.reversal_of_tx_id already exists'' AS message',
  'ALTER TABLE points_transaction ADD COLUMN reversal_of_tx_id BIGINT NULL COMMENT ''reversal source tx id'' AFTER related_reservation_no'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
