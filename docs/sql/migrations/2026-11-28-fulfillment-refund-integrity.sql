-- Track confirmed cash refunds and an active manual request on the order row.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order') AND
    NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order' AND COLUMN_NAME = 'refunded_amount'),
    'ALTER TABLE ecommerce_order.`order`
    ADD COLUMN refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN manual_refund_key VARCHAR(100) NULL', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Backfill successful historical refunds on shared MySQL.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_payment' AND TABLE_NAME = 'refund'),
    'UPDATE ecommerce_order.`order` AS orders
JOIN (
    SELECT order_no, SUM(amount) AS refunded FROM ecommerce_payment.refund
    WHERE status = 1 AND deleted = 0 GROUP BY order_no
) AS ledger ON ledger.order_no = orders.order_no
SET orders.refunded_amount = ledger.refunded
WHERE ledger.refunded > orders.refunded_amount AND ledger.refunded <= orders.total_amount', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Keep one immutable snapshot per post-close adjustment.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_payment' AND TABLE_NAME = 'daily_settlement'),
    'CREATE TABLE IF NOT EXISTS ecommerce_payment.settlement_adjustment (
    id BIGINT NOT NULL PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    total_payment_count INT NOT NULL,
    total_payment_amount DECIMAL(12,2) NOT NULL,
    total_refund_count INT NOT NULL,
    total_refund_amount DECIMAL(12,2) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_settlement_adjustment (settlement_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Preserve the largest observed partial refund even if the payment event arrives later.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_member' AND TABLE_NAME = 'member_order_finance') AND
    NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ecommerce_member' AND TABLE_NAME = 'member_order_finance' AND COLUMN_NAME = 'partial_refund_amount'),
    'ALTER TABLE ecommerce_member.member_order_finance
    ADD COLUMN partial_refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Recalculate member levels from existing net growth.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_member' AND TABLE_NAME = 'member_profile') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_member' AND TABLE_NAME = 'member_level'),
    'UPDATE ecommerce_member.member_profile AS profile
JOIN ecommerce_member.member_level AS tier ON tier.id = (
    SELECT available.id FROM ecommerce_member.member_level AS available
    WHERE available.growth_threshold <= GREATEST(profile.growth_value, 0)
    ORDER BY available.sort_order DESC, available.id DESC LIMIT 1
)
SET profile.level_id = tier.id, profile.version = profile.version + 1
WHERE NOT (profile.level_id <=> tier.id)', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Populate the product aggregates from existing visible reviews.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_product' AND TABLE_NAME = 'spu') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_product' AND TABLE_NAME = 'review'),
    'UPDATE ecommerce_product.spu AS product
LEFT JOIN (
    SELECT spu_id, COUNT(*) AS total, AVG(rating) AS average
    FROM ecommerce_product.review WHERE deleted = 0 GROUP BY spu_id
) AS ratings ON ratings.spu_id = product.id
SET product.review_count = COALESCE(ratings.total, 0), product.avg_rating = COALESCE(ratings.average, 0)
WHERE product.deleted = 0', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- The committed shipment is the durable outbound request.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order') AND
    NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order' AND COLUMN_NAME = 'outbound_state'),
    'ALTER TABLE ecommerce_logistics.shipping_order
    ADD COLUMN outbound_state VARCHAR(24) NULL,
    ADD COLUMN outbound_error VARCHAR(255) NULL,
    ADD COLUMN outbound_retry_at DATETIME NULL,
    ADD INDEX idx_shipping_outbound_retry (outbound_state, outbound_retry_at, id)', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Allow an unused tracking number to be reused after cancellation.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order' AND INDEX_NAME = 'uk_provider_tracking') AND
    NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order' AND COLUMN_NAME = 'active_tracking_no'),
    'ALTER TABLE ecommerce_logistics.shipping_order
    ADD COLUMN active_tracking_no VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN shipping_status <> 7 AND deleted = 0 THEN tracking_no ELSE NULL END) STORED,
    DROP INDEX uk_provider_tracking,
    ADD UNIQUE KEY uk_provider_active_tracking (provider_id, active_tracking_no)', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- A shipment can own only one warehouse outbound. Resolve any historical duplicates before applying.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'outbound_order') AND
    NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'outbound_order' AND INDEX_NAME = 'uk_outbound_shipping'),
    'ALTER TABLE ecommerce_warehouse.outbound_order ADD UNIQUE KEY uk_outbound_shipping (shipping_id)', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Each active outbound line owns specific physical stock rows.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'outbound_order_item'),
    'CREATE TABLE IF NOT EXISTS ecommerce_warehouse.outbound_stock_allocation (
    outbound_item_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (outbound_item_id, stock_id),
    INDEX idx_outbound_allocation_stock (stock_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Redistribute only the previously pooled locks. Physical quantities remain unchanged.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'outbound_stock_allocation') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'physical_stock'),
    'INSERT INTO ecommerce_warehouse.outbound_stock_allocation (outbound_item_id, stock_id, quantity)
WITH active_items AS (
    SELECT item.id, orders.warehouse_id, orders.created_at, item.sku_id, item.quantity
    FROM ecommerce_warehouse.outbound_order_item AS item
    JOIN ecommerce_warehouse.outbound_order AS orders ON orders.id = item.outbound_id
    WHERE item.deleted = 0 AND orders.deleted = 0 AND orders.status IN (0, 1)
),
by_item AS (
    SELECT active.id AS item_id, active.warehouse_id, active.sku_id, active.created_at,
        CAST(active.quantity AS SIGNED) - COALESCE(SUM(allocation.quantity), 0) AS missing_qty
    FROM active_items AS active
    LEFT JOIN ecommerce_warehouse.outbound_stock_allocation AS allocation ON allocation.outbound_item_id = active.id
    GROUP BY active.id, active.warehouse_id, active.sku_id, active.created_at, active.quantity
),
by_stock AS (
    SELECT stock.id AS stock_id, stock.warehouse_id, stock.sku_id,
        CAST(stock.locked_qty AS SIGNED) - COALESCE(SUM(CASE WHEN active.id IS NOT NULL THEN allocation.quantity ELSE 0 END), 0) AS free_locks
    FROM ecommerce_warehouse.physical_stock AS stock
    LEFT JOIN ecommerce_warehouse.outbound_stock_allocation AS allocation ON allocation.stock_id = stock.id
    LEFT JOIN active_items AS active ON active.id = allocation.outbound_item_id
    WHERE stock.deleted = 0 AND stock.locked_qty > 0
    GROUP BY stock.id, stock.warehouse_id, stock.sku_id, stock.locked_qty
),
demand AS (
    SELECT by_item.*, SUM(missing_qty) OVER (PARTITION BY warehouse_id, sku_id ORDER BY created_at, item_id) AS demand_end
    FROM by_item WHERE missing_qty > 0
),
supply AS (
    SELECT by_stock.*, SUM(free_locks) OVER (PARTITION BY warehouse_id, sku_id ORDER BY stock_id) AS supply_end
    FROM by_stock WHERE free_locks > 0
)
SELECT d.item_id, s.stock_id,
    LEAST(d.demand_end, s.supply_end) - GREATEST(d.demand_end - d.missing_qty, s.supply_end - s.free_locks) AS allocated_qty
FROM demand AS d JOIN supply AS s ON s.warehouse_id = d.warehouse_id AND s.sku_id = d.sku_id
WHERE d.demand_end > s.supply_end - s.free_locks AND s.supply_end > d.demand_end - d.missing_qty
ON DUPLICATE KEY UPDATE quantity = outbound_stock_allocation.quantity + VALUES(quantity)', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;

-- Make recoverable legacy orphans eligible for automatic claim reconciliation.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_order' AND TABLE_NAME = 'order_shipping_claim') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_logistics' AND TABLE_NAME = 'shipping_order') AND
    EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'ecommerce_warehouse' AND TABLE_NAME = 'outbound_order'),
    'UPDATE ecommerce_order.order_shipping_claim AS claim
JOIN ecommerce_warehouse.outbound_order AS outbound ON outbound.shipping_id = claim.shipping_id
    AND outbound.deleted = 0 AND outbound.status IN (0, 1, 4)
LEFT JOIN ecommerce_logistics.shipping_order AS shipping ON shipping.id = claim.shipping_id
    AND shipping.deleted = 0 AND shipping.shipping_status <> 7
SET claim.expires_at = NOW()
WHERE claim.managed = TRUE AND claim.expires_at IS NULL AND shipping.id IS NULL', 'DO 0');
PREPARE integrity_migration FROM @ddl;
EXECUTE integrity_migration;
DEALLOCATE PREPARE integrity_migration;
