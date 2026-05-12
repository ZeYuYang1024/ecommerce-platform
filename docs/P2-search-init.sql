-- ============================================================
-- P2 搜索服务 SQL（ecommerce_search）
-- ES 商品搜索索引同步日志
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_search
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_search;

CREATE TABLE IF NOT EXISTS sync_log (
    id            BIGINT    NOT NULL PRIMARY KEY COMMENT '雪花ID',
    entity_type   VARCHAR(32)  NOT NULL COMMENT '实体类型（product/category）',
    entity_id     BIGINT       NOT NULL COMMENT '实体ID',
    action        VARCHAR(16)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
    synced_at     DATETIME     COMMENT '同步时间',
    status        TINYINT      DEFAULT 0 COMMENT '0待同步 1已同步 2失败',
    error_msg     VARCHAR(512) COMMENT '失败原因',
    created_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES同步日志';
