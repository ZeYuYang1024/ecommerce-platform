CREATE DATABASE IF NOT EXISTS ecommerce_knowledge
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ecommerce_knowledge;

CREATE TABLE IF NOT EXISTS kb_category (
    id          BIGINT       PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    code        VARCHAR(50)  NOT NULL COMMENT '分类编码',
    parent_id   BIGINT       DEFAULT 0 COMMENT '父分类ID',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    owner_type  VARCHAR(20)  NOT NULL DEFAULT 'platform' COMMENT '归属类型: platform/merchant',
    merchant_id BIGINT       DEFAULT NULL COMMENT '商家ID，平台知识库为空',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_owner_scope (owner_type, merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分类';

CREATE TABLE IF NOT EXISTS kb_document (
    id          BIGINT       PRIMARY KEY,
    category_id BIGINT       NOT NULL COMMENT '分类ID',
    title       VARCHAR(500) NOT NULL COMMENT '文档标题',
    content     LONGTEXT     NOT NULL COMMENT '文档内容',
    source_type VARCHAR(50)  DEFAULT 'manual' COMMENT '来源类型: manual/import/auto_sync',
    status      VARCHAR(20)  DEFAULT 'draft' COMMENT '状态: draft/published/archived',
    owner_type  VARCHAR(20)  NOT NULL DEFAULT 'platform' COMMENT '归属类型: platform/merchant',
    merchant_id BIGINT       DEFAULT NULL COMMENT '商家ID，平台知识库为空',
    milvus_ids  JSON         COMMENT 'Milvus 向量 ID 列表',
    chunk_count INT          DEFAULT 0 COMMENT '分块数量',
    deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0-未删除 1-已删除',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_owner_scope (owner_type, merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文档';

CREATE TABLE IF NOT EXISTS kb_chat_session (
    id            BIGINT      PRIMARY KEY,
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    session_id    VARCHAR(64) NOT NULL COMMENT '会话标识',
    title         VARCHAR(200) COMMENT '会话标题',
    message_count INT         DEFAULT 0 COMMENT '消息数',
    create_time   DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话';

INSERT INTO kb_category (id, name, code, parent_id, sort_order, owner_type, merchant_id) VALUES
(1, '商品相关', 'product', 0, 1, 'platform', NULL),
(2, '订单相关', 'order', 0, 2, 'platform', NULL),
(3, '优惠券', 'coupon', 0, 3, 'platform', NULL),
(4, '支付相关', 'payment', 0, 4, 'platform', NULL),
(5, '平台规则', 'rule', 0, 5, 'platform', NULL),
(6, '常见问题', 'faq', 0, 6, 'platform', NULL);
