USE ecommerce_order;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'inventory_reserved'
), 'DO 0', 'ALTER TABLE ecommerce_order.`order` ADD COLUMN inventory_reserved BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE commerce_consistency FROM @ddl;
EXECUTE commerce_consistency;
DEALLOCATE PREPARE commerce_consistency;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'payment_refund_required'
), 'DO 0', 'ALTER TABLE ecommerce_order.`order` ADD COLUMN payment_refund_required BOOLEAN NOT NULL DEFAULT FALSE');
PREPARE commerce_consistency FROM @ddl;
EXECUTE commerce_consistency;
DEALLOCATE PREPARE commerce_consistency;

-- Existing open orders on a shared MySQL can use their committed inventory events.
SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'ecommerce_inventory' AND TABLE_NAME = 'inventory_event_log'
), 'UPDATE ecommerce_order.`order` AS orders
    JOIN ecommerce_inventory.inventory_event_log AS reserved ON reserved.order_no = orders.order_no COLLATE utf8mb4_0900_ai_ci
        AND reserved.topic = ''order-created'' AND reserved.status = 1 AND reserved.deleted = 0
    LEFT JOIN ecommerce_inventory.inventory_event_log AS released ON released.order_no = orders.order_no COLLATE utf8mb4_0900_ai_ci
        AND released.topic = ''order-cancelled'' AND released.status = 1 AND released.deleted = 0
    SET orders.inventory_reserved = TRUE
    WHERE orders.status IN (0, 1) AND released.id IS NULL', 'DO 0');
PREPARE commerce_consistency FROM @ddl;
EXECUTE commerce_consistency;
DEALLOCATE PREPARE commerce_consistency;

CREATE DATABASE IF NOT EXISTS ecommerce_member DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS ecommerce_member.member_order_finance (
    order_no VARCHAR(64) NOT NULL PRIMARY KEY,
    fully_refunded BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order'
) AND NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order' AND COLUMN_NAME = 'whole_order'
), 'ALTER TABLE ecommerce_logistics.shipping_order ADD COLUMN whole_order BOOLEAN NOT NULL DEFAULT FALSE', 'DO 0');
PREPARE commerce_consistency FROM @ddl;
EXECUTE commerce_consistency;
DEALLOCATE PREPARE commerce_consistency;
