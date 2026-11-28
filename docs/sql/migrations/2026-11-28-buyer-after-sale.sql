USE ecommerce_order;

CREATE TABLE IF NOT EXISTS order_after_sale (
    id BIGINT NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    merchant_id BIGINT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    status VARCHAR(24) NOT NULL,
    review_note VARCHAR(256) NULL,
    refund_no VARCHAR(32) NULL,
    failure_reason VARCHAR(256) NULL,
    refund_attempt INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_after_sale_order (order_id),
    KEY idx_after_sale_merchant_status (merchant_id, status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_after_sale' AND COLUMN_NAME = 'refund_attempt'
), 'DO 0', 'ALTER TABLE order_after_sale ADD COLUMN refund_attempt INT NOT NULL DEFAULT 0');
PREPARE after_sale_attempt FROM @ddl;
EXECUTE after_sale_attempt;
DEALLOCATE PREPARE after_sale_attempt;

CREATE TABLE IF NOT EXISTS order_shipping_claim (
    order_id BIGINT NOT NULL,
    shipping_no VARCHAR(64) NOT NULL,
    shipping_id BIGINT NULL,
    managed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    PRIMARY KEY (order_id, shipping_no),
    KEY idx_shipping_claim_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_shipping_claim' AND COLUMN_NAME = 'expires_at'
), 'DO 0', 'ALTER TABLE order_shipping_claim
    ADD COLUMN shipping_id BIGINT NULL, ADD COLUMN managed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN expires_at DATETIME NULL,
    ADD KEY idx_shipping_claim_expiry (expires_at)');
SET @migrate_lease = @ddl <> 'DO 0';
PREPARE after_sale_claim_lease FROM @ddl;
EXECUTE after_sale_claim_lease;
DEALLOCATE PREPARE after_sale_claim_lease;
UPDATE order_shipping_claim SET expires_at = NOW() + INTERVAL 30 MINUTE WHERE @migrate_lease;

-- Preserve fulfillment already started by the legacy shipping flow on the local shared MySQL.
SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order'
), 'INSERT INTO order_shipping_claim (order_id, shipping_no)
    SELECT order_id, shipping_no FROM ecommerce_logistics.shipping_order WHERE deleted = 0
    ON DUPLICATE KEY UPDATE expires_at = NULL', 'DO 0');
PREPARE after_sale_shipping FROM @ddl;
EXECUTE after_sale_shipping;
DEALLOCATE PREPARE after_sale_shipping;

CREATE TABLE IF NOT EXISTS ecommerce_inventory.inventory_order_lock (
    order_no VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
