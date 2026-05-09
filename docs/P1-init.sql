-- ============================================================
-- P1 数据库初始化 SQL（仅新增，不含 P2 重复部分）
-- 前置：已执行 P2-multi-role.sql（merchant 库 + admin_user 扩展 + spu 扩展）
-- 新增：ecommerce_order、ecommerce_payment
-- 修复：merchant_audit 补充缺失字段（P2 建表缺少 updated_at / deleted）
-- ============================================================

-- ============================
-- 0. 修复 merchant_audit 表（P2 建表时缺少字段）
-- ============================


USE ecommerce_merchant;
ALTER TABLE merchant_audit
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      ADD COLUMN deleted TINYINT DEFAULT 0;
-- ============================
-- 1. 新建订单库 ecommerce_order
-- ============================

CREATE DATABASE IF NOT EXISTS ecommerce_order
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_order;

CREATE TABLE IF NOT EXISTS `order` (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    order_no          VARCHAR(32)   NOT NULL COMMENT '订单号（yyyyMMddHHmmss+4位序列）',
    user_id           BIGINT        NOT NULL COMMENT '用户ID',
    total_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
    status            TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=已发货 3=已完成 4=已取消',
    receiver_name     VARCHAR(64)   COMMENT '收货人',
    receiver_phone    VARCHAR(20)   COMMENT '收货电话',
    receiver_address  VARCHAR(256)  COMMENT '收货地址',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

CREATE TABLE IF NOT EXISTS order_item (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    order_id          BIGINT        NOT NULL COMMENT '订单ID',
    order_no          VARCHAR(32)   NOT NULL COMMENT '订单号',
    sku_id            BIGINT        COMMENT 'SKU ID',
    spu_id            BIGINT        COMMENT 'SPU ID',
    name              VARCHAR(256)  COMMENT '商品名称',
    image             VARCHAR(512)  COMMENT '商品图片',
    price             DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity          INT           NOT NULL DEFAULT 1 COMMENT '数量',
    total_price       DECIMAL(10,2) NOT NULL COMMENT '小计（price * quantity）',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================
-- 2. 新建支付库 ecommerce_payment
-- ============================

CREATE DATABASE IF NOT EXISTS ecommerce_payment
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_payment;

CREATE TABLE IF NOT EXISTS payment (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    payment_no        VARCHAR(32)   NOT NULL COMMENT '支付单号（PAY+yyyyMMddHHmmss+序列）',
    order_no          VARCHAR(32)   NOT NULL COMMENT '关联订单号',
    order_id          BIGINT        COMMENT '关联订单ID',
    user_id           BIGINT        NOT NULL COMMENT '用户ID',
    amount            DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status            TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=退款中 3=已退款 4=已关闭',
    pay_method        VARCHAR(32)   DEFAULT 'wx_jsapi' COMMENT '支付方式',
    paid_at           DATETIME      COMMENT '支付时间',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_payment_no (payment_no),
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

CREATE TABLE IF NOT EXISTS refund (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    refund_no         VARCHAR(32)   NOT NULL COMMENT '退款单号（REF+yyyyMMddHHmmss+序列）',
    payment_id        BIGINT        NOT NULL COMMENT '关联支付ID',
    order_no          VARCHAR(32)   NOT NULL COMMENT '关联订单号',
    amount            DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    reason            VARCHAR(512)  COMMENT '退款原因',
    status            TINYINT       NOT NULL DEFAULT 0 COMMENT '0=处理中 1=已退款 2=已拒绝',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    INDEX idx_payment_id (payment_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';
