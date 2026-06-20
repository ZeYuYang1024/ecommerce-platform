-- ============================================================
-- ecommerce-warehouse Phase 2: Warehouse management tables
-- Source: docs/superpowers/specs/2026-06-20-logistics-warehouse-design.md
-- Created: 2026-06-20
-- ============================================================

-- 1. warehouse
CREATE TABLE warehouse (
    id                BIGINT PRIMARY KEY,
    warehouse_name    VARCHAR(64)  NOT NULL COMMENT '仓库名称',
    warehouse_code    VARCHAR(32)  NOT NULL COMMENT '仓库编码',
    warehouse_type    TINYINT DEFAULT 1 COMMENT '0平台仓/1商家仓',
    stock_mode        TINYINT DEFAULT 0 COMMENT '0轻仓模式/1托管库存模式',
    merchant_id       BIGINT       COMMENT '所属商家ID',
    province          VARCHAR(32),
    city              VARCHAR(32),
    district          VARCHAR(32),
    address           VARCHAR(255),
    contact_name      VARCHAR(32),
    contact_phone     VARCHAR(32),
    status            TINYINT DEFAULT 0 COMMENT '0停用/1启用',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_warehouse_code (warehouse_code),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. warehouse_zone
CREATE TABLE warehouse_zone (
    id                BIGINT PRIMARY KEY,
    warehouse_id      BIGINT       NOT NULL,
    zone_name         VARCHAR(64)  NOT NULL,
    zone_code         VARCHAR(32)  NOT NULL,
    zone_type         TINYINT DEFAULT 0 COMMENT '0存储区/1拣货区/2退货区',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_warehouse_zone_code (warehouse_id, zone_code),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. warehouse_bin
CREATE TABLE warehouse_bin (
    id                BIGINT PRIMARY KEY,
    zone_id           BIGINT       NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    bin_code          VARCHAR(32)  NOT NULL,
    bin_type          TINYINT DEFAULT 0 COMMENT '0普通/1重型/2冷藏',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_warehouse_bin_code (warehouse_id, bin_code),
    INDEX idx_zone_id (zone_id),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. inbound_order
CREATE TABLE inbound_order (
    id                BIGINT PRIMARY KEY,
    inbound_no        VARCHAR(32)  NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    inbound_type      TINYINT DEFAULT 0 COMMENT '0采购入库/1退货入库/2调拨入库/3盘点调整',
    source_order_no   VARCHAR(32),
    status            TINYINT DEFAULT 0 COMMENT '0待收货/1已收货/2已上架/3已完成',
    merchant_id       BIGINT,
    remark            VARCHAR(255),
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_inbound_no (inbound_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. inbound_order_item
CREATE TABLE inbound_order_item (
    id                BIGINT PRIMARY KEY,
    inbound_id        BIGINT       NOT NULL,
    sku_id            BIGINT       NOT NULL,
    quantity          INT DEFAULT 0 COMMENT '预期数量',
    received_qty      INT DEFAULT 0 COMMENT '实收数量',
    bin_id            BIGINT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    INDEX idx_inbound_id (inbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. outbound_order
CREATE TABLE outbound_order (
    id                BIGINT PRIMARY KEY,
    outbound_no       VARCHAR(32)  NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    outbound_type     TINYINT DEFAULT 0 COMMENT '0销售出库/1调拨出库/2盘点调整',
    shipping_id       BIGINT       COMMENT '关联发货单ID',
    status            TINYINT DEFAULT 0 COMMENT '0待拣货/1拣货中/2已出库/3已交运',
    merchant_id       BIGINT,
    remark            VARCHAR(255),
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_outbound_no (outbound_no),
    INDEX idx_warehouse_id (warehouse_id),
    INDEX idx_shipping_id (shipping_id),
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. outbound_order_item
CREATE TABLE outbound_order_item (
    id                BIGINT PRIMARY KEY,
    outbound_id       BIGINT       NOT NULL,
    sku_id            BIGINT       NOT NULL,
    quantity          INT DEFAULT 0 COMMENT '预期数量',
    picked_qty        INT DEFAULT 0 COMMENT '已拣数量',
    shipped_qty       INT DEFAULT 0 COMMENT '已出数量',
    bin_id            BIGINT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    INDEX idx_outbound_id (outbound_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. physical_stock
CREATE TABLE physical_stock (
    id                BIGINT PRIMARY KEY,
    warehouse_id      BIGINT       NOT NULL,
    sku_id            BIGINT       NOT NULL,
    bin_id            BIGINT       COMMENT '货位ID',
    quantity          INT DEFAULT 0 COMMENT '实物数量',
    locked_qty        INT DEFAULT 0 COMMENT '已锁定待出库数量',
    available_qty     INT DEFAULT 0 COMMENT '可用数量，冗余字段，必须由统一 StockService 原子维护',
    safety_stock      INT DEFAULT 0 COMMENT '安全库存阈值',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_warehouse_sku_bin (warehouse_id, sku_id, bin_id),
    INDEX idx_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. stock_check
CREATE TABLE stock_check (
    id                BIGINT PRIMARY KEY,
    check_no          VARCHAR(32)  NOT NULL,
    warehouse_id      BIGINT       NOT NULL,
    status            TINYINT DEFAULT 0 COMMENT '0盘点中/1已完成/2差异待处理',
    merchant_id       BIGINT,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    UNIQUE KEY uk_check_no (check_no),
    INDEX idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. stock_check_item
CREATE TABLE stock_check_item (
    id                BIGINT PRIMARY KEY,
    check_id          BIGINT       NOT NULL,
    sku_id            BIGINT       NOT NULL,
    bin_id            BIGINT,
    system_qty        INT DEFAULT 0,
    actual_qty        INT DEFAULT 0,
    diff_qty          INT DEFAULT 0,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    INDEX idx_check_id (check_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
