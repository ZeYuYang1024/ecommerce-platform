-- Merchant marketing tenant migration
-- Scope:
--   1. ecommerce_coupon.coupon_template
--   2. ecommerce_seckill.seckill_session
--   3. ecommerce_seckill.seckill_item
--
-- This script is idempotent and safe to re-run.

-- ------------------------------------------------------------------
-- ecommerce_coupon
-- ------------------------------------------------------------------
USE ecommerce_coupon;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'coupon_template'
      AND COLUMN_NAME = 'merchant_id'
  ),
  'SELECT ''coupon_template.merchant_id already exists'' AS message',
  'ALTER TABLE coupon_template ADD COLUMN merchant_id BIGINT NULL COMMENT ''merchant id'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'coupon_template'
      AND INDEX_NAME = 'idx_coupon_template_merchant_id'
  ),
  'SELECT ''idx_coupon_template_merchant_id already exists'' AS message',
  'ALTER TABLE coupon_template ADD INDEX idx_coupon_template_merchant_id (merchant_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------------
-- ecommerce_seckill
-- ------------------------------------------------------------------
USE ecommerce_seckill;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'seckill_session'
      AND COLUMN_NAME = 'merchant_id'
  ),
  'SELECT ''seckill_session.merchant_id already exists'' AS message',
  'ALTER TABLE seckill_session ADD COLUMN merchant_id BIGINT NULL COMMENT ''merchant id'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'seckill_session'
      AND INDEX_NAME = 'idx_seckill_session_merchant_id'
  ),
  'SELECT ''idx_seckill_session_merchant_id already exists'' AS message',
  'ALTER TABLE seckill_session ADD INDEX idx_seckill_session_merchant_id (merchant_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'seckill_item'
      AND COLUMN_NAME = 'merchant_id'
  ),
  'SELECT ''seckill_item.merchant_id already exists'' AS message',
  'ALTER TABLE seckill_item ADD COLUMN merchant_id BIGINT NULL COMMENT ''merchant id'' AFTER id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'seckill_item'
      AND INDEX_NAME = 'idx_seckill_item_merchant_id'
  ),
  'SELECT ''idx_seckill_item_merchant_id already exists'' AS message',
  'ALTER TABLE seckill_item ADD INDEX idx_seckill_item_merchant_id (merchant_id)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'coupon_template' AS table_name, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ecommerce_coupon'
  AND TABLE_NAME = 'coupon_template'
  AND COLUMN_NAME = 'merchant_id'
UNION ALL
SELECT 'seckill_session' AS table_name, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ecommerce_seckill'
  AND TABLE_NAME = 'seckill_session'
  AND COLUMN_NAME = 'merchant_id'
UNION ALL
SELECT 'seckill_item' AS table_name, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ecommerce_seckill'
  AND TABLE_NAME = 'seckill_item'
  AND COLUMN_NAME = 'merchant_id';
