-- ============================================================
-- P2 多角色扩展 SQL
-- 平台管理员 + 运营人员 + 商家 + C端用户
-- ============================================================

-- ============================
-- 1. 扩展 admin_user 表
-- ============================

USE ecommerce_auth;

ALTER TABLE admin_user
    ADD COLUMN type VARCHAR(20) DEFAULT 'super_admin' COMMENT 'super_admin / ops / merchant',
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT '商家用户关联的店铺ID';

-- 更新初始管理员类型
UPDATE admin_user SET type = 'super_admin' WHERE id = 1;

-- ============================
-- 2. 新建商家库
-- ============================

CREATE DATABASE IF NOT EXISTS ecommerce_merchant
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ecommerce_merchant;

CREATE TABLE merchant (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name            VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    logo            VARCHAR(512)  COMMENT 'Logo URL',
    contact_name    VARCHAR(64)   COMMENT '联系人',
    contact_phone   VARCHAR(20)   COMMENT '联系电话',
    business_license VARCHAR(512) COMMENT '营业执照URL',
    status          TINYINT       DEFAULT 0 COMMENT '0=待审核 1=通过 2=驳回 3=关停',
    reason          VARCHAR(512)  COMMENT '审核意见/驳回原因',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

CREATE TABLE merchant_audit (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    merchant_id     BIGINT        NOT NULL,
    auditor_id      BIGINT        COMMENT '审核人（admin_user.id）',
    action          TINYINT       NOT NULL COMMENT '1=通过 2=驳回 3=关停',
    comment         VARCHAR(512),
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家审核记录';

-- ============================
-- 3. 商品表扩展（自营 vs 商家）
-- ============================

USE ecommerce_product;

ALTER TABLE spu
    ADD COLUMN merchant_id BIGINT DEFAULT NULL COMMENT 'NULL=自营，有值=商家商品';

ALTER TABLE spu
    ADD INDEX idx_merchant_id (merchant_id);
