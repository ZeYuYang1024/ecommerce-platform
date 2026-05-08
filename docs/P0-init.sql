-- ============================================================
-- P0 数据库初始化脚本
-- 创建日期：2026-05-08
-- 执行方式：mysql -uroot -proot < P0-init.sql
-- ============================================================

-- ============================
-- 1. 创建数据库
-- ============================

CREATE DATABASE IF NOT EXISTS ecommerce_auth
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ecommerce_user
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ecommerce_product
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ecommerce_inventory
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- ============================
-- 2. ecommerce_auth 认证库
-- ============================

USE ecommerce_auth;

CREATE TABLE user (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(256)  NOT NULL COMMENT 'MD5加密',
    phone       VARCHAR(20),
    avatar      VARCHAR(512),
    status      TINYINT       DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端用户表';

CREATE TABLE admin_user (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(256)  NOT NULL COMMENT 'MD5加密',
    avatar      VARCHAR(512),
    status      TINYINT       DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE role (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(64)   NOT NULL,
    description VARCHAR(256),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE permission (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(128)  NOT NULL COMMENT '权限标识',
    type        VARCHAR(20)   COMMENT 'menu/button/api',
    parent_id   BIGINT        DEFAULT 0,
    path        VARCHAR(256)  COMMENT '路由路径',
    icon        VARCHAR(64),
    sort        INT           DEFAULT 0,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE admin_user_role (
    id            BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    admin_user_id BIGINT NOT NULL,
    role_id       BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员角色关联表';

CREATE TABLE role_permission (
    id            BIGINT NOT NULL PRIMARY KEY COMMENT '雪花ID',
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================
-- 3. ecommerce_user 用户库
-- ============================

USE ecommerce_user;

CREATE TABLE address (
    id             BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id        BIGINT        NOT NULL,
    receiver_name  VARCHAR(64)   NOT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20)   NOT NULL COMMENT '收货电话',
    province       VARCHAR(64),
    city           VARCHAR(64),
    district       VARCHAR(64),
    detail         VARCHAR(256)  NOT NULL COMMENT '详细地址',
    is_default     TINYINT       DEFAULT 0 COMMENT '0=否 1=是',
    created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT       DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================
-- 4. ecommerce_product 商品库
-- ============================

USE ecommerce_product;

CREATE TABLE category (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    parent_id   BIGINT        DEFAULT 0,
    level       INT           DEFAULT 1 COMMENT '层级',
    sort        INT           DEFAULT 0,
    icon        VARCHAR(512),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE brand (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    logo        VARCHAR(512),
    description VARCHAR(256),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

CREATE TABLE spu (
    id           BIGINT         NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name         VARCHAR(256)   NOT NULL,
    category_id  BIGINT         NOT NULL,
    brand_id     BIGINT,
    description  TEXT,
    main_image   VARCHAR(512)   COMMENT '主图URL',
    images       TEXT           COMMENT '图片JSON数组',
    detail       TEXT           COMMENT '富文本详情',
    status       TINYINT        DEFAULT 0 COMMENT '0=下架 1=上架',
    avg_rating   DECIMAL(3,2)   DEFAULT 0 COMMENT '平均评分',
    review_count INT            DEFAULT 0 COMMENT '评论数',
    created_at   DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT        DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

CREATE TABLE sku (
    id             BIGINT         NOT NULL PRIMARY KEY COMMENT '雪花ID',
    spu_id         BIGINT         NOT NULL,
    name           VARCHAR(256)   NOT NULL,
    spec           JSON           COMMENT '规格JSON',
    price          DECIMAL(10,2)  NOT NULL COMMENT '售价',
    original_price DECIMAL(10,2)  COMMENT '原价',
    image          VARCHAR(512),
    created_at     DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT        DEFAULT 0,
    INDEX idx_spu_id (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

CREATE TABLE review (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    spu_id      BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    username    VARCHAR(64),
    order_id    BIGINT,
    rating      TINYINT       NOT NULL COMMENT '1-5星',
    content     VARCHAR(1024),
    images      TEXT          COMMENT '图片JSON数组',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_spu_id (spu_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';

-- ============================
-- 5. ecommerce_inventory 库存库
-- ============================

USE ecommerce_inventory;

CREATE TABLE stock (
    id              BIGINT    NOT NULL PRIMARY KEY COMMENT '雪花ID',
    sku_id          BIGINT    NOT NULL,
    total_stock     INT       DEFAULT 0  COMMENT '总库存',
    locked_stock    INT       DEFAULT 0  COMMENT '锁定库存（下单未支付）',
    available_stock INT       DEFAULT 0  COMMENT '可用库存',
    version         INT       DEFAULT 0  COMMENT '乐观锁版本号',
    created_at      DATETIME  DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT   DEFAULT 0,
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- ============================
-- 6. 初始数据：默认管理员
-- ============================

USE ecommerce_auth;

-- 密码: admin123 (MD5)
-- 密码: admin123 (MD5: 0192023a7bbd73250516f069df18b500)
INSERT INTO admin_user (id, username, password, status)
VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', 1);
