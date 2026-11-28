SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'client_request_id'
), 'ALTER TABLE ecommerce_order.`order` ADD COLUMN client_request_id VARCHAR(64) NULL, ADD COLUMN checkout_fingerprint CHAR(64) NULL, ADD UNIQUE KEY uk_order_checkout (user_id, client_request_id), ADD KEY idx_order_expiry (status, created_at, id)', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order_item'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order_item' AND COLUMN_NAME = 'merchant_scope_recorded'
), 'ALTER TABLE ecommerce_order.`order_item` ADD COLUMN merchant_id BIGINT NULL, ADD COLUMN merchant_scope_recorded BOOLEAN NOT NULL DEFAULT FALSE, ADD KEY idx_order_item_merchant (merchant_id, order_id)', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_payment' AND TABLE_NAME = 'reconciliation'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_payment' AND TABLE_NAME = 'reconciliation' AND COLUMN_NAME = 'processing_count'
), 'ALTER TABLE ecommerce_payment.`reconciliation` ADD COLUMN processing_count INT NOT NULL DEFAULT 0, ADD COLUMN failure_reason VARCHAR(256) NULL', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_product' AND TABLE_NAME = 'review'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_product' AND TABLE_NAME = 'review' AND COLUMN_NAME = 'verified_order_id'
), 'ALTER TABLE ecommerce_product.`review` ADD COLUMN verified_order_id BIGINT NULL, ADD UNIQUE KEY uk_review_verified_order_spu (verified_order_id, spu_id)', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'stock_check'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'stock_check' AND COLUMN_NAME = 'approved_by'
), 'ALTER TABLE ecommerce_warehouse.`stock_check` ADD COLUMN approved_by BIGINT NULL, ADD COLUMN approved_at DATETIME NULL', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

-- Shared deployments can preserve the current catalog owner for legacy order lines.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order_item') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_product' AND TABLE_NAME = 'spu'),
    'UPDATE ecommerce_order.order_item AS line
JOIN ecommerce_product.spu AS product ON product.id = line.spu_id AND product.deleted = 0
SET line.merchant_id = product.merchant_id, line.merchant_scope_recorded = TRUE
WHERE line.merchant_scope_recorded = FALSE AND line.deleted = 0', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

-- Existing stocktakes used 0 as a default even when the operator had never counted a line.
SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'stock_check_item'
        AND COLUMN_NAME = 'actual_qty' AND COLUMN_DEFAULT IS NOT NULL
), 'ALTER TABLE ecommerce_warehouse.stock_check_item
    MODIFY actual_qty INT NULL DEFAULT NULL, MODIFY diff_qty INT NULL DEFAULT NULL', 'DO 0');
SET @needs_stocktake_reset = @ddl != 'DO 0';
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(@needs_stocktake_reset, 'UPDATE ecommerce_warehouse.stock_check_item AS line
    JOIN ecommerce_warehouse.stock_check AS header ON header.id = line.check_id
    SET line.actual_qty = NULL, line.diff_qty = NULL, header.status = 0
    WHERE header.status IN (0, 2) AND line.actual_qty = 0 AND line.diff_qty = 0', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_coupon' AND TABLE_NAME = 'coupon_template') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_coupon' AND TABLE_NAME = 'user_coupon'),
    'UPDATE ecommerce_coupon.coupon_template AS template
LEFT JOIN (
    SELECT template_id, COUNT(*) AS issued FROM ecommerce_coupon.user_coupon
    WHERE deleted = 0 GROUP BY template_id
) AS claims ON claims.template_id = template.id
SET template.remaining_count = GREATEST(CAST(template.total_count AS SIGNED) - CAST(COALESCE(claims.issued, 0) AS SIGNED), 0)
WHERE template.deleted = 0 AND template.total_count IS NOT NULL
    AND NOT (template.remaining_count <=> GREATEST(CAST(template.total_count AS SIGNED) - CAST(COALESCE(claims.issued, 0) AS SIGNED), 0))', 'DO 0');
PREPARE checkout_operations FROM @ddl;
EXECUTE checkout_operations;
DEALLOCATE PREPARE checkout_operations;
