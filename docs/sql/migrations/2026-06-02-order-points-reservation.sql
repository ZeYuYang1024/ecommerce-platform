USE ecommerce_order;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order'
      AND COLUMN_NAME = 'original_amount'
  ),
  'SELECT ''order.original_amount already exists'' AS message',
  'ALTER TABLE `order` ADD COLUMN original_amount DECIMAL(10,2) NULL COMMENT ''original order amount'' AFTER total_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order'
      AND COLUMN_NAME = 'points_reservation_no'
  ),
  'SELECT ''order.points_reservation_no already exists'' AS message',
  'ALTER TABLE `order` ADD COLUMN points_reservation_no VARCHAR(32) NULL COMMENT ''points reservation number'' AFTER original_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order'
      AND COLUMN_NAME = 'points_used'
  ),
  'SELECT ''order.points_used already exists'' AS message',
  'ALTER TABLE `order` ADD COLUMN points_used INT NULL COMMENT ''points used for deduction'' AFTER points_reservation_no'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order'
      AND COLUMN_NAME = 'points_deduction_amount'
  ),
  'SELECT ''order.points_deduction_amount already exists'' AS message',
  'ALTER TABLE `order` ADD COLUMN points_deduction_amount DECIMAL(10,2) NULL COMMENT ''points deduction amount'' AFTER points_used'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := IF (
  EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order'
      AND COLUMN_NAME = 'points_deduction_ratio'
  ),
  'SELECT ''order.points_deduction_ratio already exists'' AS message',
  'ALTER TABLE `order` ADD COLUMN points_deduction_ratio INT NULL COMMENT ''points per 1 yuan'' AFTER points_deduction_amount'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
