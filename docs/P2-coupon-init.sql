-- ============================================================
-- P2 优惠券服务 SQL（ecommerce_coupon）
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_coupon
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_coupon;

CREATE TABLE IF NOT EXISTS coupon_template (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name            VARCHAR(128) NOT NULL COMMENT '券名称',
    type            VARCHAR(32)  NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/FLAT',
    min_amount      DECIMAL(10,2) COMMENT '最低消费金额',
    discount_amount DECIMAL(10,2) COMMENT '减免金额/折扣率',
    discount_rate   DECIMAL(5,2)  COMMENT '折扣率（type=DISCOUNT 时使用，如 0.85 表示85折）',
    total_count     INT          DEFAULT 0 COMMENT '总发行量',
    remaining_count INT          DEFAULT 0 COMMENT '剩余数量',
    per_user_limit  INT          DEFAULT 1 COMMENT '每人限领',
    start_time      DATETIME     COMMENT '有效期开始',
    end_time        DATETIME     COMMENT '有效期结束',
    status          TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS user_coupon (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    template_id     BIGINT       NOT NULL COMMENT '优惠券模板ID',
    status          TINYINT      DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    order_no        VARCHAR(32)  COMMENT '使用的订单号',
    used_at         DATETIME     COMMENT '使用时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_template_id (template_id),
    INDEX idx_user_template (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';
