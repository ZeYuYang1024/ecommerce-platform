-- ============================================================
-- P1 补全 — 对账 & 日结结算表
-- 新增 reconciliation / reconciliation_detail / daily_settlement 三张表
-- 放在 ecommerce_payment 库下，不重复已有表
-- ============================================================

USE ecommerce_payment;

-- ============================
-- 1. 对账批次表
-- ============================

CREATE TABLE IF NOT EXISTS reconciliation (
    id                  BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    batch_no            VARCHAR(32)   NOT NULL COMMENT '对账批次号（REC+yyyyMMddHHmmss+序列）',
    start_date          DATETIME      COMMENT '对账开始时间',
    end_date            DATETIME      COMMENT '对账结束时间',
    total_order_count   INT           DEFAULT 0 COMMENT '订单总数',
    total_payment_count INT           DEFAULT 0 COMMENT '支付单总数',
    matched_count       INT           DEFAULT 0 COMMENT '匹配成功数',
    unmatched_count     INT           DEFAULT 0 COMMENT '匹配失败数（差异）',
    status              TINYINT       DEFAULT 0 COMMENT '0=进行中 1=已完成 2=失败',
    created_at          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账批次表';

-- ============================
-- 2. 对账明细表
-- ============================

CREATE TABLE IF NOT EXISTS reconciliation_detail (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    reconciliation_id BIGINT        NOT NULL COMMENT '关联对账批次ID',
    record_type       VARCHAR(16)   NOT NULL COMMENT '记录类型: ORDER / PAYMENT',
    order_no          VARCHAR(32)   COMMENT '订单号',
    payment_no        VARCHAR(32)   COMMENT '支付单号',
    amount            DECIMAL(10,2) COMMENT '金额',
    record_status     TINYINT       COMMENT '记录状态（订单status或支付status）',
    match_status      VARCHAR(16)   NOT NULL COMMENT '匹配结果: MATCHED / ORDER_ONLY / PAYMENT_ONLY / AMOUNT_MISMATCH / STATUS_MISMATCH',
    diff_reason       VARCHAR(256)  COMMENT '差异原因',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    INDEX idx_reconciliation_id (reconciliation_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账明细表';

-- ============================
-- 3. 日终结算表
-- ============================

CREATE TABLE IF NOT EXISTS daily_settlement (
    id                    BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    settlement_date       DATE          NOT NULL COMMENT '结算日期',
    total_order_count     INT           DEFAULT 0 COMMENT '当日订单数',
    total_order_amount    DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日订单总额',
    total_payment_count   INT           DEFAULT 0 COMMENT '当日支付笔数',
    total_payment_amount  DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日支付总额',
    total_refund_count    INT           DEFAULT 0 COMMENT '当日退款笔数',
    total_refund_amount   DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日退款总额',
    net_amount            DECIMAL(12,2) DEFAULT 0.00 COMMENT '净收入（支付-退款）',
    status                TINYINT       DEFAULT 0 COMMENT '0=草稿 1=已确认',
    created_at            DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_settlement_date (settlement_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日终结算表';
