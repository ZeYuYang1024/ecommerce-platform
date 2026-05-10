-- ============================================================
-- P3 Schema 修复 — 补齐旧表缺失的 BaseEntity 列
-- 使用存储过程安全执行，跳过已存在的列
-- ============================================================

SET NAMES utf8mb4;

DELIMITER $$
DROP PROCEDURE IF EXISTS add_column_if_not_exists$$
CREATE PROCEDURE add_column_if_not_exists(IN db_name VARCHAR(64), IN tbl_name VARCHAR(64), IN col_name VARCHAR(64), IN col_def VARCHAR(256))
BEGIN
    SET @db = db_name, @tbl = tbl_name, @col = col_name, @def = col_def;
    SET @sql = CONCAT('ALTER TABLE ', @db, '.', @tbl, ' ADD COLUMN ', @col, ' ', @def);
    SET @cnt = 0;
    SELECT COUNT(*) INTO @cnt FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND COLUMN_NAME = @col;
    IF @cnt = 0 THEN
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================
-- ecommerce_auth 库
-- ============================
CALL add_column_if_not_exists('ecommerce_auth', 'role', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_auth', 'role', 'deleted', 'TINYINT DEFAULT 0');

CALL add_column_if_not_exists('ecommerce_auth', 'permission', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_auth', 'permission', 'deleted', 'TINYINT DEFAULT 0');

CALL add_column_if_not_exists('ecommerce_auth', 'admin_user_role', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_auth', 'admin_user_role', 'deleted', 'TINYINT DEFAULT 0');

CALL add_column_if_not_exists('ecommerce_auth', 'role_permission', 'created_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_auth', 'role_permission', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_auth', 'role_permission', 'deleted', 'TINYINT DEFAULT 0');

-- ============================
-- ecommerce_product 库
-- ============================
CALL add_column_if_not_exists('ecommerce_product', 'review', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_product', 'review', 'deleted', 'TINYINT DEFAULT 0');

CALL add_column_if_not_exists('ecommerce_product', 'category', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_product', 'category', 'deleted', 'TINYINT DEFAULT 0');

CALL add_column_if_not_exists('ecommerce_product', 'brand', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_product', 'brand', 'deleted', 'TINYINT DEFAULT 0');

-- ============================
-- ecommerce_order 库
-- ============================
CALL add_column_if_not_exists('ecommerce_order', 'order_item', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_order', 'order_item', 'deleted', 'TINYINT DEFAULT 0');

-- ============================
-- ecommerce_merchant 库
-- ============================
CALL add_column_if_not_exists('ecommerce_merchant', 'merchant_audit', 'updated_at', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP');
CALL add_column_if_not_exists('ecommerce_merchant', 'merchant_audit', 'deleted', 'TINYINT DEFAULT 0');

-- 清理
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
