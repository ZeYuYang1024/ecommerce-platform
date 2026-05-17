-- Live merchant marketing seed data for tenant smoke.
-- Merchant account: m_2053170012063142000 / admin123
-- Merchant ID: 2053411485924855808

USE ecommerce_coupon;

DELETE FROM coupon_template
WHERE id IN (
    2060517101000000001,
    2060517101000000002,
    2060517101000000091
);

INSERT INTO coupon_template (
    id,
    merchant_id,
    name,
    type,
    min_amount,
    discount_amount,
    discount_rate,
    total_count,
    remaining_count,
    per_user_limit,
    start_time,
    end_time,
    status,
    created_at,
    updated_at,
    deleted
) VALUES
(
    2060517101000000001,
    2053411485924855808,
    'merchant-live-coupon-A-20260517',
    'FULL_REDUCTION',
    199.00,
    30.00,
    NULL,
    120,
    120,
    1,
    '2026-05-17 00:00:00',
    '2026-05-31 23:59:59',
    1,
    '2026-05-17 12:40:00',
    '2026-05-17 12:40:00',
    0
),
(
    2060517101000000002,
    2053411485924855808,
    'merchant-live-coupon-B-20260517',
    'DISCOUNT',
    0.00,
    NULL,
    0.85,
    80,
    80,
    1,
    '2026-05-17 00:00:00',
    '2026-05-31 23:59:59',
    1,
    '2026-05-17 12:41:00',
    '2026-05-17 12:41:00',
    0
),
(
    2060517101000000091,
    2053840826341134336,
    'noise-merchant-coupon-20260517',
    'FLAT',
    0.00,
    15.00,
    NULL,
    50,
    50,
    1,
    '2026-05-17 00:00:00',
    '2026-05-31 23:59:59',
    1,
    '2026-05-17 12:42:00',
    '2026-05-17 12:42:00',
    0
);

USE ecommerce_seckill;

DELETE FROM seckill_item
WHERE id IN (
    2060517103000000001,
    2060517103000000002,
    2060517103000000091
);

DELETE FROM seckill_session
WHERE id IN (
    2060517102000000001,
    2060517102000000091
);

INSERT INTO seckill_session (
    id,
    merchant_id,
    name,
    start_time,
    end_time,
    status,
    created_at,
    updated_at,
    deleted
) VALUES
(
    2060517102000000001,
    2053411485924855808,
    'merchant-live-session-20260517',
    '2026-05-17 12:00:00',
    '2026-05-17 23:59:59',
    1,
    '2026-05-17 12:43:00',
    '2026-05-17 12:43:00',
    0
),
(
    2060517102000000091,
    2053840826341134336,
    'noise-session-20260517',
    '2026-05-17 12:00:00',
    '2026-05-17 23:59:59',
    1,
    '2026-05-17 12:44:00',
    '2026-05-17 12:44:00',
    0
);

INSERT INTO seckill_item (
    id,
    merchant_id,
    session_id,
    spu_id,
    sku_id,
    name,
    original_price,
    seckill_price,
    stock_count,
    remaining_count,
    status,
    created_at,
    updated_at,
    deleted
) VALUES
(
    2060517103000000001,
    2053411485924855808,
    2060517102000000001,
    2055867290058100736,
    2055867290087460864,
    'merchant-live-seckill-smoke-standard',
    499.00,
    349.00,
    30,
    30,
    1,
    '2026-05-17 12:45:00',
    '2026-05-17 12:45:00',
    0
),
(
    2060517103000000002,
    2053411485924855808,
    2060517102000000001,
    2055867458144833536,
    2055867458165805056,
    'merchant-live-seckill-seed-standard',
    699.00,
    519.00,
    18,
    18,
    1,
    '2026-05-17 12:46:00',
    '2026-05-17 12:46:00',
    0
),
(
    2060517103000000091,
    2053840826341134336,
    2060517102000000091,
    2055867290058100736,
    2055867290087460864,
    'noise-seckill-item-20260517',
    399.00,
    299.00,
    8,
    8,
    1,
    '2026-05-17 12:47:00',
    '2026-05-17 12:47:00',
    0
);
