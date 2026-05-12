-- ============================================================
-- P3 通知服务 SQL（ecommerce_notification）
-- ============================================================

CREATE DATABASE IF NOT EXISTS ecommerce_notification
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_notification;

CREATE TABLE IF NOT EXISTS notification_template (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    template_code   VARCHAR(64)  NOT NULL COMMENT '模板编码（ORDER_CREATED/ORDER_PAID等）',
    name            VARCHAR(128) NOT NULL COMMENT '模板名称',
    type            VARCHAR(32)  NOT NULL COMMENT 'SMS/EMAIL/MINI_PROGRAM/APP_PUSH',
    title           VARCHAR(256) COMMENT '通知标题',
    content         TEXT         COMMENT '通知内容模板（支持占位符）',
    status          TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板';

CREATE TABLE IF NOT EXISTS notification_log (
    id              BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id         BIGINT       COMMENT '用户ID',
    template_id     BIGINT       COMMENT '模板ID',
    type            VARCHAR(32)  NOT NULL COMMENT '通知类型',
    recipient       VARCHAR(256) COMMENT '接收人（手机号/邮箱/openid）',
    title           VARCHAR(256) COMMENT '通知标题',
    content         TEXT         COMMENT '实际发送内容',
    status          TINYINT      DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
    error_msg       VARCHAR(512) COMMENT '失败原因',
    sent_at         DATETIME     COMMENT '发送时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送日志';
