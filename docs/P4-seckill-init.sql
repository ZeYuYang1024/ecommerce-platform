-- ============================================================
-- P4 秒杀服务 SQL（ecommerce_seckill）
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_seckill
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_seckill;

CREATE TABLE IF NOT EXISTS seckill_session (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name            VARCHAR(128) NOT NULL COMMENT '场次名称',
    start_time      DATETIME     NOT NULL COMMENT '开始时间',
    end_time        DATETIME     NOT NULL COMMENT '结束时间',
    status          TINYINT      DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次';

CREATE TABLE IF NOT EXISTS seckill_item (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    session_id      BIGINT       NOT NULL COMMENT '场次ID',
    spu_id          BIGINT       NOT NULL COMMENT '商品SPU ID',
    sku_id          BIGINT       NOT NULL COMMENT '商品SKU ID',
    name            VARCHAR(256) NOT NULL COMMENT '秒杀商品名称',
    original_price  DECIMAL(10,2) NOT NULL COMMENT '原价',
    seckill_price   DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
    stock_count     INT          NOT NULL COMMENT '秒杀库存总量',
    remaining_count INT          NOT NULL COMMENT '剩余库存',
    status          TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品';
