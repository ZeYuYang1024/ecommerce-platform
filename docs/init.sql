-- ============================================================
-- E-Commerce Platform — 完整初始化 SQL
-- 包含：建库、建表、测试数据、RBAC种子数据
-- 执行方式：mysql -uroot -proot < init.sql
-- ============================================================

SET NAMES utf8mb4;

-- ============================
-- 1. 创建所有数据库
-- ============================

CREATE DATABASE IF NOT EXISTS ecommerce_auth       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_user       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_product    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_inventory  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_merchant   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_order      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_payment    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_coupon     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_search     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS ecommerce_seckill    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================
-- 2. ecommerce_auth — 认证/权限
-- ============================

USE ecommerce_auth;

CREATE TABLE IF NOT EXISTS user (
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

CREATE TABLE IF NOT EXISTS admin_user (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    username    VARCHAR(64)   NOT NULL,
    password    VARCHAR(256)  NOT NULL COMMENT 'MD5加密',
    avatar      VARCHAR(512),
    type        VARCHAR(20)   DEFAULT 'super_admin' COMMENT 'super_admin/ops/merchant',
    merchant_id BIGINT        DEFAULT NULL COMMENT '商家管理员关联店铺ID',
    status      TINYINT       DEFAULT 1 COMMENT '0=禁用 1=正常',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS role (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(64)   NOT NULL,
    description VARCHAR(256),
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS permission (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)   NOT NULL,
    code        VARCHAR(128)  NOT NULL COMMENT '权限标识',
    type        VARCHAR(20)   COMMENT 'menu/button/api',
    parent_id   BIGINT        DEFAULT 0,
    path        VARCHAR(256)  COMMENT '路由路径',
    icon        VARCHAR(64),
    sort        INT           DEFAULT 0,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS admin_user_role (
    id            BIGINT   NOT NULL PRIMARY KEY COMMENT '雪花ID',
    admin_user_id BIGINT   NOT NULL,
    role_id       BIGINT   NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT  DEFAULT 0,
    UNIQUE KEY uk_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员角色关联表';

CREATE TABLE IF NOT EXISTS role_permission (
    id            BIGINT   NOT NULL PRIMARY KEY COMMENT '雪花ID',
    role_id       BIGINT   NOT NULL,
    permission_id BIGINT   NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT  DEFAULT 0,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ============================
-- 3. ecommerce_user — 用户服务
-- ============================

USE ecommerce_user;

CREATE TABLE IF NOT EXISTS address (
    id             BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id        BIGINT       NOT NULL,
    receiver_name  VARCHAR(64)  NOT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20)  NOT NULL COMMENT '收货电话',
    province       VARCHAR(64),
    city           VARCHAR(64),
    district       VARCHAR(64),
    detail         VARCHAR(256) NOT NULL COMMENT '详细地址',
    is_default     TINYINT      DEFAULT 0 COMMENT '0=否 1=是',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================
-- 4. ecommerce_product — 商品服务
-- ============================

USE ecommerce_product;

CREATE TABLE IF NOT EXISTS category (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)  NOT NULL,
    parent_id   BIGINT       DEFAULT 0,
    level       INT          DEFAULT 1 COMMENT '层级',
    sort        INT          DEFAULT 0,
    icon        VARCHAR(512),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

CREATE TABLE IF NOT EXISTS brand (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(64)  NOT NULL,
    logo        VARCHAR(512),
    description VARCHAR(256),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='品牌表';

CREATE TABLE IF NOT EXISTS spu (
    id           BIGINT         NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name         VARCHAR(256)   NOT NULL,
    category_id  BIGINT         NOT NULL,
    brand_id     BIGINT,
    merchant_id  BIGINT         DEFAULT NULL COMMENT 'NULL=自营, 有值=商家商品',
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
    INDEX idx_status (status),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

CREATE TABLE IF NOT EXISTS sku (
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

CREATE TABLE IF NOT EXISTS review (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    spu_id      BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    username    VARCHAR(64),
    order_id    BIGINT,
    rating      TINYINT       NOT NULL COMMENT '1-5星',
    content     VARCHAR(1024),
    images      TEXT          COMMENT '图片JSON数组',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_spu_id (spu_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';

-- ============================
-- 5. ecommerce_inventory — 库存服务
-- ============================

USE ecommerce_inventory;

CREATE TABLE IF NOT EXISTS stock (
    id              BIGINT   NOT NULL PRIMARY KEY COMMENT '雪花ID',
    sku_id          BIGINT   NOT NULL,
    total_stock     INT      DEFAULT 0 COMMENT '总库存',
    locked_stock    INT      DEFAULT 0 COMMENT '锁定库存(下单未支付)',
    available_stock INT      DEFAULT 0 COMMENT '可用库存',
    version         INT      DEFAULT 0 COMMENT '乐观锁版本号',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT  DEFAULT 0,
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- ============================
-- 6. ecommerce_merchant — 商家服务
-- ============================

USE ecommerce_merchant;

CREATE TABLE IF NOT EXISTS merchant (
    id               BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name             VARCHAR(128)  NOT NULL COMMENT '店铺名称',
    logo             VARCHAR(512)  COMMENT 'Logo URL',
    contact_name     VARCHAR(64)   COMMENT '联系人',
    contact_phone    VARCHAR(20)   COMMENT '联系电话',
    business_license VARCHAR(512)  COMMENT '营业执照URL',
    status           TINYINT       DEFAULT 0 COMMENT '0=待审核 1=通过 2=驳回 3=关停',
    reason           VARCHAR(512)  COMMENT '审核意见/驳回原因',
    created_at       DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家表';

CREATE TABLE IF NOT EXISTS merchant_audit (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    merchant_id BIGINT       NOT NULL,
    auditor_id  BIGINT       COMMENT '审核人(admin_user.id)',
    action      TINYINT      NOT NULL COMMENT '1=通过 2=驳回 3=关停',
    comment     VARCHAR(512),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0,
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家审核记录';

-- ============================
-- 7. ecommerce_order — 订单服务
-- ============================

USE ecommerce_order;

CREATE TABLE IF NOT EXISTS `order` (
    id                BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    order_no          VARCHAR(32)   NOT NULL COMMENT '订单号',
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
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    order_id        BIGINT        NOT NULL COMMENT '订单ID',
    order_no        VARCHAR(32)   NOT NULL COMMENT '订单号',
    sku_id          BIGINT        COMMENT 'SKU ID',
    spu_id          BIGINT        COMMENT 'SPU ID',
    name            VARCHAR(256)  COMMENT '商品名称',
    image           VARCHAR(512)  COMMENT '商品图片',
    price           DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity        INT           NOT NULL DEFAULT 1 COMMENT '数量',
    total_price     DECIMAL(10,2) NOT NULL COMMENT '小计',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_order_id (order_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================
-- 8. ecommerce_payment — 支付服务
-- ============================

USE ecommerce_payment;

CREATE TABLE IF NOT EXISTS payment (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    payment_no  VARCHAR(32)   NOT NULL COMMENT '支付单号',
    order_no    VARCHAR(32)   NOT NULL COMMENT '关联订单号',
    order_id    BIGINT        COMMENT '关联订单ID',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    amount      DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=待支付 1=已支付 2=退款中 3=已退款 4=已关闭',
    pay_method  VARCHAR(32)   DEFAULT 'wx_jsapi' COMMENT '支付方式',
    paid_at     DATETIME      COMMENT '支付时间',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_payment_no (payment_no),
    INDEX idx_order_no (order_no),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

CREATE TABLE IF NOT EXISTS refund (
    id          BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    refund_no   VARCHAR(32)   NOT NULL COMMENT '退款单号',
    payment_id  BIGINT        NOT NULL COMMENT '关联支付ID',
    order_no    VARCHAR(32)   NOT NULL COMMENT '关联订单号',
    amount      DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    reason      VARCHAR(512)  COMMENT '退款原因',
    status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0=处理中 1=已退款 2=已拒绝',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_payment_id (payment_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款表';

CREATE TABLE IF NOT EXISTS reconciliation (
    id                  BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花ID',
    batch_no            VARCHAR(32) NOT NULL COMMENT '对账批次号',
    start_date          DATETIME    COMMENT '对账开始时间',
    end_date            DATETIME    COMMENT '对账结束时间',
    total_order_count   INT         DEFAULT 0 COMMENT '订单总数',
    total_payment_count INT         DEFAULT 0 COMMENT '支付单总数',
    matched_count       INT         DEFAULT 0 COMMENT '匹配成功数',
    unmatched_count     INT         DEFAULT 0 COMMENT '匹配失败数(差异)',
    status              TINYINT     DEFAULT 0 COMMENT '0=进行中 1=已完成 2=失败',
    created_at          DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT     DEFAULT 0,
    UNIQUE INDEX uk_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账批次表';

CREATE TABLE IF NOT EXISTS reconciliation_detail (
    id                BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    reconciliation_id BIGINT       NOT NULL COMMENT '关联对账批次ID',
    record_type       VARCHAR(16)  NOT NULL COMMENT '记录类型: ORDER/PAYMENT',
    order_no          VARCHAR(32)  COMMENT '订单号',
    payment_no        VARCHAR(32)  COMMENT '支付单号',
    amount            DECIMAL(10,2) COMMENT '金额',
    record_status     TINYINT      COMMENT '记录状态',
    match_status      VARCHAR(16)  NOT NULL COMMENT '匹配结果: MATCHED/ORDER_ONLY/PAYMENT_ONLY/AMOUNT_MISMATCH/STATUS_MISMATCH',
    diff_reason       VARCHAR(256) COMMENT '差异原因',
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT      DEFAULT 0,
    INDEX idx_reconciliation_id (reconciliation_id),
    INDEX idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对账明细表';

CREATE TABLE IF NOT EXISTS daily_settlement (
    id                  BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    settlement_date     DATE          NOT NULL COMMENT '结算日期',
    total_order_count   INT           DEFAULT 0 COMMENT '当日订单数',
    total_order_amount  DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日订单总额',
    total_payment_count INT           DEFAULT 0 COMMENT '当日支付笔数',
    total_payment_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日支付总额',
    total_refund_count  INT           DEFAULT 0 COMMENT '当日退款笔数',
    total_refund_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日退款总额',
    net_amount          DECIMAL(12,2) DEFAULT 0.00 COMMENT '净收入(支付-退款)',
    status              TINYINT       DEFAULT 0 COMMENT '0=草稿 1=已确认',
    created_at          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT       DEFAULT 0,
    UNIQUE INDEX uk_settlement_date (settlement_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='日终结算表';

-- ============================
-- 9. ecommerce_coupon — 优惠券服务
-- ============================

USE ecommerce_coupon;

CREATE TABLE IF NOT EXISTS coupon_template (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name            VARCHAR(128)  NOT NULL COMMENT '券名称',
    type            VARCHAR(32)   NOT NULL COMMENT 'FULL_REDUCTION/DISCOUNT/FLAT',
    min_amount      DECIMAL(10,2) COMMENT '最低消费金额',
    discount_amount DECIMAL(10,2) COMMENT '减免金额',
    discount_rate   DECIMAL(5,2)  COMMENT '折扣率(type=DISCOUNT时使用, 如0.85=85折)',
    total_count     INT           DEFAULT 0 COMMENT '总发行量',
    remaining_count INT           DEFAULT 0 COMMENT '剩余数量',
    per_user_limit  INT           DEFAULT 1 COMMENT '每人限领',
    start_time      DATETIME      COMMENT '有效期开始',
    end_time        DATETIME      COMMENT '有效期结束',
    status          TINYINT       DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS user_coupon (
    id           BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id      BIGINT       NOT NULL COMMENT '用户ID',
    template_id  BIGINT       NOT NULL COMMENT '优惠券模板ID',
    status       TINYINT      DEFAULT 0 COMMENT '0未使用 1已使用 2已过期',
    order_no     VARCHAR(32)  COMMENT '使用的订单号',
    used_at      DATETIME     COMMENT '使用时间',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_template_id (template_id),
    INDEX idx_user_template (user_id, template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

-- ============================
-- 10. ecommerce_search — 搜索服务
-- ============================

USE ecommerce_search;

CREATE TABLE IF NOT EXISTS sync_log (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    entity_type VARCHAR(32)  NOT NULL COMMENT '实体类型(product/category)',
    entity_id   BIGINT       NOT NULL COMMENT '实体ID',
    action      VARCHAR(16)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE',
    synced_at   DATETIME     COMMENT '同步时间',
    status      TINYINT      DEFAULT 0 COMMENT '0待同步 1已同步 2失败',
    error_msg   VARCHAR(512) COMMENT '失败原因',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ES同步日志';

-- ============================
-- 11. ecommerce_notification — 通知服务
-- ============================

USE ecommerce_notification;

CREATE TABLE IF NOT EXISTS notification_template (
    id             BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    template_code  VARCHAR(64)  NOT NULL COMMENT '模板编码',
    name           VARCHAR(128) NOT NULL COMMENT '模板名称',
    type           VARCHAR(32)  NOT NULL COMMENT 'SMS/EMAIL/MINI_PROGRAM/APP_PUSH',
    title          VARCHAR(256) COMMENT '通知标题',
    content        TEXT         COMMENT '通知内容模板(支持占位符)',
    status         TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板';

CREATE TABLE IF NOT EXISTS notification_log (
    id           BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    user_id      BIGINT       COMMENT '用户ID',
    template_id  BIGINT       COMMENT '模板ID',
    type         VARCHAR(32)  NOT NULL COMMENT '通知类型',
    recipient    VARCHAR(256) COMMENT '接收人',
    title        VARCHAR(256) COMMENT '通知标题',
    content      TEXT         COMMENT '实际发送内容',
    status       TINYINT      DEFAULT 0 COMMENT '0待发送 1已发送 2失败',
    error_msg    VARCHAR(512) COMMENT '失败原因',
    sent_at      DATETIME     COMMENT '发送时间',
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送日志';

-- ============================
-- 12. ecommerce_seckill — 秒杀服务
-- ============================

USE ecommerce_seckill;

CREATE TABLE IF NOT EXISTS seckill_session (
    id          BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花ID',
    name        VARCHAR(128) NOT NULL COMMENT '场次名称',
    start_time  DATETIME     NOT NULL COMMENT '开始时间',
    end_time    DATETIME     NOT NULL COMMENT '结束时间',
    status      TINYINT      DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀场次';

CREATE TABLE IF NOT EXISTS seckill_item (
    id              BIGINT         NOT NULL PRIMARY KEY COMMENT '雪花ID',
    session_id      BIGINT         NOT NULL COMMENT '场次ID',
    spu_id          BIGINT         NOT NULL COMMENT '商品SPU ID',
    sku_id          BIGINT         NOT NULL COMMENT '商品SKU ID',
    name            VARCHAR(256)   NOT NULL COMMENT '秒杀商品名称',
    original_price  DECIMAL(10,2)  NOT NULL COMMENT '原价',
    seckill_price   DECIMAL(10,2)  NOT NULL COMMENT '秒杀价',
    stock_count     INT            NOT NULL COMMENT '秒杀库存总量',
    remaining_count INT            NOT NULL COMMENT '剩余库存',
    status          TINYINT        DEFAULT 1 COMMENT '0禁用 1启用',
    created_at      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT        DEFAULT 0,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品';

-- ==================================================================
-- 以下为测试数据
-- ==================================================================

-- ============================
-- 13. 分类数据（仿京东）
-- ============================

USE ecommerce_product;

INSERT IGNORE INTO category (id, name, parent_id, level, sort) VALUES
-- 一级分类
(1,  '家用电器',     0, 1, 1),
(2,  '手机 / 数码',  0, 1, 2),
(3,  '电脑 / 办公',  0, 1, 3),
(4,  '家居 / 家具',  0, 1, 4),
(5,  '服饰 / 内衣',  0, 1, 5),
(6,  '美妆 / 护肤',  0, 1, 6),
(7,  '食品 / 生鲜',  0, 1, 7),
(8,  '图书 / 文娱',  0, 1, 8),
(9,  '运动 / 户外',  0, 1, 9),
(10, '汽车 / 用品',  0, 1, 10),
-- 二级分类
(11, '大家电',   1, 2, 1),  (12, '厨房电器', 1, 2, 2),  (13, '生活电器', 1, 2, 3),
(14, '手机',     2, 2, 1),  (15, '摄影摄像', 2, 2, 2),  (16, '智能穿戴', 2, 2, 3),
(17, '笔记本',   3, 2, 1),  (18, '台式机',   3, 2, 2),  (19, '办公耗材', 3, 2, 3),
(20, '客厅家具', 4, 2, 1),  (21, '卧室家具', 4, 2, 2),
(22, '男装', 5, 2, 1),      (23, '女装', 5, 2, 2),      (24, '内衣', 5, 2, 3),
(25, '面部护肤', 6, 2, 1),  (26, '彩妆',     6, 2, 2),
(27, '休闲零食', 7, 2, 1),  (28, '生鲜果蔬', 7, 2, 2),  (29, '酒水饮料', 7, 2, 3),
(30, '文学小说', 8, 2, 1),  (31, '少儿图书', 8, 2, 2),
(32, '运动鞋服', 9, 2, 1),  (33, '健身器材', 9, 2, 2),
(34, '车载电器', 10, 2, 1), (35, '汽车装饰', 10, 2, 2);

-- ============================
-- 14. 品牌数据
-- ============================

INSERT IGNORE INTO brand (id, name, description) VALUES
(1, '海尔',     '全球领先的家电品牌'),
(2, '美的',     '智慧生活解决方案'),
(3, '华为',     '中国科技品牌'),
(4, '小米',     '智能科技品牌'),
(5, '苹果',     '美国科技公司'),
(6, '联想',     '全球PC领导品牌'),
(7, '戴尔',     '美国电脑品牌'),
(8, '耐克',     '全球运动品牌'),
(9, '阿迪达斯', '德国运动品牌'),
(10, '欧莱雅',  '法国美妆品牌'),
(11, '三只松鼠', '互联网坚果品牌'),
(12, '宜家',    '瑞典家居品牌'),
(13, '索尼',    '日本电子品牌'),
(14, '格力',    '中国空调品牌');

-- ============================
-- 15. 商品数据 (20 个 SPU + 对应 SKU)
-- ============================

-- 海尔冰箱
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(1, '海尔双开门冰箱 BCD-500', 11, 1, '500升大容量，风冷无霜，一级能效', '<p>海尔双开门冰箱，500L大容量</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(1, 1, 'BCD-500 银灰色', '{"颜色":"银灰色"}', 3999.00, 4599.00),
(2, 1, 'BCD-500 白色',   '{"颜色":"白色"}',   3899.00, 4499.00);

-- 美的微波炉
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(2, '美的智能微波炉 M3-L239', 12, 2, '23L大容量，变频加热，智能菜单', '<p>美的智能微波炉</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(3, 2, 'M3-L239 黑色', '{"颜色":"黑色"}', 599.00, 699.00),
(4, 2, 'M3-L239 白色', '{"颜色":"白色"}', 579.00, 679.00);

-- 格力空调
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(3, '格力变频空调 KFR-35GW 1.5匹', 11, 14, '新一级能效，变频冷暖，自清洁', '<p>格力1.5匹变频空调</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(5, 3, 'KFR-35GW 标准款', '{"类型":"标准款"}', 3299.00, 3799.00),
(6, 3, 'KFR-35GW WiFi款', '{"类型":"WiFi智能款"}', 3599.00, 4099.00);

-- 华为 Mate 70 Pro
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(4, '华为 Mate 70 Pro', 14, 3, '麒麟芯片，卫星通信，XMAGE影像', '<p>华为旗舰手机 Mate 70 Pro</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(7, 4, 'Mate 70 Pro 12+256GB 雅丹黑', '{"颜色":"雅丹黑","存储":"12+256GB"}', 6499.00, 6999.00),
(8, 4, 'Mate 70 Pro 12+512GB 雅丹黑', '{"颜色":"雅丹黑","存储":"12+512GB"}', 7499.00, 7999.00),
(9, 4, 'Mate 70 Pro 12+512GB 白沙银', '{"颜色":"白沙银","存储":"12+512GB"}', 7499.00, 7999.00);

-- 小米 15 Ultra
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(5, '小米 15 Ultra', 14, 4, '骁龙8Gen4，徕卡光学，120W快充', '<p>小米旗舰手机 15 Ultra</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(10, 5, '15 Ultra 12+256GB 黑色', '{"颜色":"黑色","存储":"12+256GB"}', 4999.00, 5299.00),
(11, 5, '15 Ultra 16+512GB 白色', '{"颜色":"白色","存储":"16+512GB"}', 5799.00, 6099.00);

-- 小米 Buds 5 Pro
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(6, '小米 Buds 5 Pro', 16, 4, '主动降噪，Hi-Res音质，36小时续航', '<p>小米旗舰无线耳机</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(12, 6, 'Buds 5 Pro 黑色', '{"颜色":"黑色"}', 799.00, 899.00),
(13, 6, 'Buds 5 Pro 白色', '{"颜色":"白色"}', 799.00, 899.00);

-- 联想 ThinkPad X1 Carbon
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(7, '联想 ThinkPad X1 Carbon', 17, 6, '14英寸商务旗舰，i7处理器，1TB固态', '<p>ThinkPad X1 Carbon Gen12</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(14, 7, 'X1C i7-155H 16GB+512GB', '{"配置":"i7-155H 16+512"}', 9999.00, 10999.00),
(15, 7, 'X1C i7-155H 32GB+1TB',  '{"配置":"i7-155H 32+1T"}',  11999.00, 12999.00);

-- 戴尔 XPS 14
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(8, '戴尔 XPS 14', 17, 7, '14英寸OLED屏，Ultra9处理器', '<p>戴尔 XPS 14 2025款</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(16, 8, 'XPS14 Ultra9 16GB+512GB', '{"配置":"Ultra9 16+512"}', 10999.00, 11999.00),
(17, 8, 'XPS14 Ultra9 32GB+1TB',  '{"配置":"Ultra9 32+1T"}',  12999.00, 13999.00);

-- 华硕主板
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(9, '华硕 ROG MAXIMUS Z890 HERO', 18, NULL, 'Z890芯片组，LGA1851，DDR5', '<p>ROG旗舰主板</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(18, 9, 'Z890 HERO 标准版', '{"版本":"标准版"}', 4999.00, 5499.00);

-- 宜家沙发
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(10, '北欧风布艺沙发 三人位', 20, 12, '简约北欧风，高回弹海绵，实木框架', '<p>北欧简约三人位沙发</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(19, 10, '三人位 浅灰色', '{"颜色":"浅灰色","规格":"三人位"}', 2999.00, 3599.00),
(20, 10, '三人位 深蓝色', '{"颜色":"深蓝色","规格":"三人位"}', 2999.00, 3599.00);

-- 宜家床垫
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(11, '宜家独立弹簧床垫 1.8m', 21, 12, '独立袋装弹簧，天然乳胶层，透气面料', '<p>宜家高端床垫</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(21, 11, '1.8m 标准款', '{"规格":"1.8m×2.0m","厚度":"22cm"}', 3999.00, 4999.00),
(22, 11, '1.8m 加厚款', '{"规格":"1.8m×2.0m","厚度":"28cm"}', 4999.00, 5999.00);

-- 男士夹克
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(12, '商务休闲立领夹克', 22, NULL, '含棉面料，立体剪裁，商务休闲两穿', '<p>春季商务休闲夹克</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(23, 12, '夹克 黑色 M', '{"颜色":"黑色","尺码":"M"}', 459.00, 599.00),
(24, 12, '夹克 黑色 L', '{"颜色":"黑色","尺码":"L"}', 459.00, 599.00),
(25, 12, '夹克 卡其色 L', '{"颜色":"卡其色","尺码":"L"}', 459.00, 599.00);

-- 法式连衣裙
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(13, '法式复古碎花连衣裙', 23, NULL, 'V领设计，收腰显瘦，雪纺面料', '<p>夏季法式碎花连衣裙</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(26, 13, '连衣裙 碎花蓝 S', '{"颜色":"碎花蓝","尺码":"S"}', 299.00, 399.00),
(27, 13, '连衣裙 碎花蓝 M', '{"颜色":"碎花蓝","尺码":"M"}', 299.00, 399.00),
(28, 13, '连衣裙 碎花粉 M', '{"颜色":"碎花粉","尺码":"M"}', 299.00, 399.00);

-- 耐克跑鞋
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(14, '耐克 Air Zoom Pegasus 42', 32, 8, '全掌Zoom气垫，Flyknit鞋面，轻量缓震', '<p>耐克飞马42代跑鞋</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(29, 14, 'Pegasus42 黑白 40', '{"颜色":"黑白","尺码":"40"}', 899.00, 999.00),
(30, 14, 'Pegasus42 黑白 42', '{"颜色":"黑白","尺码":"42"}', 899.00, 999.00),
(31, 14, 'Pegasus42 全黑 42', '{"颜色":"全黑","尺码":"42"}', 899.00, 999.00);

-- 阿迪达斯跑鞋
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(15, '阿迪达斯 Ultraboost 5.0 女款', 32, 9, 'Boost中底，Primeknit编织，运动休闲', '<p>Adidas UB5.0女款跑鞋</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(32, 15, 'UB5.0 白色 37', '{"颜色":"白色","尺码":"37"}', 999.00, 1199.00),
(33, 15, 'UB5.0 白色 38', '{"颜色":"白色","尺码":"38"}', 999.00, 1199.00);

-- 欧莱雅防晒
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(16, '欧莱雅小金管防晒霜 SPF50+', 25, 10, '高倍防晒，清爽不油腻，麦色滤光科技', '<p>欧莱雅小金管防晒</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(34, 16, '小金管 30ml', '{"规格":"30ml"}', 129.00, 159.00),
(35, 16, '小金管 50ml', '{"规格":"50ml"}', 189.00, 229.00);

-- 欧莱雅口红
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(17, '欧莱雅小钢笔唇釉', 26, 10, '哑光雾面，持久不脱色，轻薄显色', '<p>欧莱雅小钢笔唇釉</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(36, 17, '唇釉 #130 枫叶红', '{"色号":"#130 枫叶红"}', 99.00, 129.00),
(37, 17, '唇釉 #145 豆沙粉', '{"色号":"#145 豆沙粉"}', 99.00, 129.00);

-- 三只松鼠
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(18, '三只松鼠坚果大礼包 1688g', 27, 11, '9袋混合坚果，年货送礼，每日坚果', '<p>三只松鼠年货坚果大礼包</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(38, 18, '坚果礼包 1688g', '{"规格":"1688g"}', 168.00, 228.00);

-- 华为手表
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(19, '华为 WATCH GT 5 Pro', 16, 3, '钛合金表壳，14天续航，ECG心电分析', '<p>华为智能手表GT5 Pro</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(39, 19, 'GT5 Pro 钛灰色', '{"颜色":"钛灰色","表带":"氟橡胶"}', 2788.00, 2988.00),
(40, 19, 'GT5 Pro 钛灰色 皮表带', '{"颜色":"钛灰色","表带":"真皮"}', 2988.00, 3188.00);

-- 罗技键盘
INSERT IGNORE INTO spu (id, name, category_id, brand_id, description, detail, status) VALUES
(20, '罗技 MX Keys S 无线键盘', 19, NULL, '全尺寸，智能背光，多设备切换，USB-C充电', '<p>罗技旗舰办公键盘</p>', 1);
INSERT IGNORE INTO sku (id, spu_id, name, spec, price, original_price) VALUES
(41, 20, 'MX Keys S 石墨黑', '{"颜色":"石墨黑"}', 799.00, 899.00),
(42, 20, 'MX Keys S 珍珠白', '{"颜色":"珍珠白"}', 799.00, 899.00);

-- ============================
-- 16. 库存数据
-- ============================

USE ecommerce_inventory;

INSERT IGNORE INTO stock (id, sku_id, total_stock, locked_stock, available_stock, version) VALUES
(1,  1,  200, 0, 200, 0), (2,  2,  150, 0, 150, 0),
(3,  3,  500, 0, 500, 0), (4,  4,  450, 0, 450, 0),
(5,  5,  100, 0, 100, 0), (6,  6,  80,  0, 80,  0),
(7,  7,  300, 0, 300, 0), (8,  8,  200, 0, 200, 0), (9,  9,  150, 0, 150, 0),
(10, 10, 400, 0, 400, 0), (11, 11, 300, 0, 300, 0),
(12, 12, 600, 0, 600, 0), (13, 13, 500, 0, 500, 0),
(14, 14, 80,  0, 80,  0), (15, 15, 50,  0, 50,  0),
(16, 16, 60,  0, 60,  0), (17, 17, 30,  0, 30,  0),
(18, 18, 40,  0, 40,  0),
(19, 19, 30,  0, 30,  0), (20, 20, 25,  0, 25,  0),
(21, 21, 50,  0, 50,  0), (22, 22, 30,  0, 30,  0),
(23, 23, 200, 0, 200, 0), (24, 24, 250, 0, 250, 0), (25, 25, 180, 0, 180, 0),
(26, 26, 150, 0, 150, 0), (27, 27, 200, 0, 200, 0), (28, 28, 180, 0, 180, 0),
(29, 29, 300, 0, 300, 0), (30, 30, 350, 0, 350, 0), (31, 31, 250, 0, 250, 0),
(32, 32, 200, 0, 200, 0), (33, 33, 220, 0, 220, 0),
(34, 34, 800, 0, 800, 0), (35, 35, 600, 0, 600, 0),
(36, 36, 500, 0, 500, 0), (37, 37, 450, 0, 450, 0),
(38, 38, 1000,0, 1000,0),
(39, 39, 150, 0, 150, 0), (40, 40, 100, 0, 100, 0),
(41, 41, 300, 0, 300, 0), (42, 42, 280, 0, 280, 0);

-- ============================
-- 17. 管理员 & 测试用户
-- ============================

USE ecommerce_auth;

-- 密码: admin123 (MD5: 0192023a7bbd73250516f069df18b500)
INSERT IGNORE INTO admin_user (id, username, password, status, type)
VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', 1, 'super_admin');

-- 密码: test123 (MD5: cc03e747a6afbbcbf8be7668acfebee5)
INSERT IGNORE INTO user (id, username, password, phone, status)
VALUES (10001, 'testuser', 'cc03e747a6afbbcbf8be7668acfebee5', '13800000000', 1);

-- ============================
-- 18. RBAC 预设角色 + 权限
-- ============================

-- 角色
INSERT IGNORE INTO role (id, name, code, description) VALUES
(1, '超级管理员', 'super_admin', '平台最高权限，管理所有功能和数据'),
(2, '运营人员',   'ops',         '管理商品、订单、支付、对账，不能审核商家和管理用户'),
(3, '商家管理员', 'merchant',    '管理自己店铺的商品');

-- 权限（按模块分组）
INSERT IGNORE INTO permission (id, name, code, type, parent_id, path, icon, sort) VALUES
-- 数据概览
(1, '数据概览', 'dashboard', 'menu', 0, '/dashboard', 'DataAnalysis', 1),
-- 商家管理
(10, '商家管理', 'merchants',       'menu', 0,  '/merchants', 'Shop',   2),
(11, '商家列表', 'merchants:list',  'menu', 10, '/merchants', NULL,     1),
(12, '商家审核', 'merchants:audit', 'button', 10, NULL,        NULL,    2),
-- 商品运营
(20, '商品管理', 'products',     'menu', 0,  '/products',   'Goods',         3),
(21, '商品列表', 'products:list', 'menu', 20, '/products',   NULL,            1),
(22, '商品编辑', 'products:edit', 'button', 20, NULL,         NULL,           2),
(23, '类目管理', 'categories',   'menu', 0,  '/categories', 'Grid',          4),
(24, '品牌管理', 'brands',       'menu', 0,  '/brands',     'Collection',    5),
(25, '评论管理', 'reviews',      'menu', 0,  '/reviews',    'ChatDotRound',  6),
(26, '库存管理', 'inventory',    'menu', 0,  '/inventory',  'Box',           7),
-- 交易管理
(30, '订单管理', 'orders',   'menu', 0, '/orders',   'Document', 8),
(31, '支付管理', 'payments', 'menu', 0, '/payments', 'Money',    9),
-- 财务管理
(40, '对账管理', 'reconciliation', 'menu', 0, '/reconciliation', 'RefreshRight', 10),
(41, '日终结算', 'settlement',     'menu', 0, '/settlement',     'TrendCharts',  11),
-- 用户与权限
(50, '用户管理', 'users',       'menu', 0, '/users',       'User', 12),
(51, '角色管理', 'roles',       'menu', 0, '/roles',       'Key',  13),
(52, '权限管理', 'permissions', 'menu', 0, '/permissions', 'Lock', 14);

-- super_admin: 所有权限
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 1, id FROM permission;

-- ops: 商品/交易/财务 (不含商家管理 10-12, 用户管理 50-52)
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 2, id FROM permission
WHERE id NOT IN (10, 11, 12, 50, 51, 52);

-- merchant: 仅商品管理
INSERT IGNORE INTO role_permission (id, role_id, permission_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 3, id FROM permission
WHERE id IN (20, 21, 22);

-- 管理员分配 super_admin 角色
INSERT IGNORE INTO admin_user_role (id, admin_user_id, role_id)
SELECT FLOOR(RAND() * 9000000000000000000) + 1000000000000000000, 1, 1;

-- ============================
-- 19. E2E 测试地址
-- ============================

USE ecommerce_user;

INSERT IGNORE INTO address (id, user_id, receiver_name, receiver_phone, province, city, district, detail, is_default)
VALUES (20001, 10001, '测试用户', '13800000000', '北京市', '北京市', '朝阳区', '望京测试地址100号', 1);
