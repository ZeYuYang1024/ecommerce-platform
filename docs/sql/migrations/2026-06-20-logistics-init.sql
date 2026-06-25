CREATE TABLE logistics_provider (
    id                BIGINT PRIMARY KEY,
    provider_code     VARCHAR(32)  NOT NULL COMMENT '快递公司编码',
    provider_name     VARCHAR(64)  NOT NULL COMMENT '快递公司名称',
    provider_logo     VARCHAR(255) COMMENT 'LOGO URL',
    customer_account  VARCHAR(128) COMMENT '月结账号/客户号',
    api_key           VARCHAR(256) COMMENT '加密存储',
    api_secret        VARCHAR(256) COMMENT '加密存储',
    aggregation_code  VARCHAR(32)  COMMENT '聚合平台内部编码',
    support_waybill   TINYINT DEFAULT 0 COMMENT '0否/1是',
    status            TINYINT DEFAULT 0 COMMENT '0停用/1启用',
    priority          INT DEFAULT 0 COMMENT '排序优先级',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流公司';

ALTER TABLE logistics_provider ADD UNIQUE KEY uk_provider_code (provider_code);

CREATE TABLE shipping_order (
    id                BIGINT PRIMARY KEY,
    shipping_no       VARCHAR(32)  NOT NULL COMMENT '发货单号',
    client_request_id VARCHAR(64)  NOT NULL COMMENT '客户端幂等键',
    order_id          BIGINT       NOT NULL COMMENT '订单ID',
    order_no          VARCHAR(32)  NOT NULL COMMENT '订单号',
    warehouse_id      BIGINT       COMMENT '发货仓ID',
    provider_id       BIGINT       COMMENT '物流公司ID',
    provider_code     VARCHAR(32)  COMMENT '物流公司编码冗余',
    tracking_no       VARCHAR(64)  COMMENT '运单号',
    dispatch_type     TINYINT DEFAULT 0 COMMENT '0手工/1电子面单/2批量导入',
    source_type       TINYINT DEFAULT 0 COMMENT '0后台/1商家/2系统自动',
    shipping_fee      DECIMAL(10,2) DEFAULT 0 COMMENT '实收运费',
    shipping_status   TINYINT DEFAULT 0 COMMENT '0待交运/1已交运/2运输中/3派送中/4已签收/5异常/6已退回',
    sender_info       JSON         COMMENT '寄件人信息',
    receiver_info     JSON         COMMENT '收件人信息',
    package_weight    INT DEFAULT 0 COMMENT '克',
    package_size      VARCHAR(64)  COMMENT 'LxWxH(cm)',
    waybill_url       VARCHAR(512) COMMENT '面单URL',
    last_trace_time   DATETIME     COMMENT '最近轨迹时间',
    last_trace_desc   VARCHAR(512) COMMENT '最近轨迹描述',
    shipped_at        DATETIME     COMMENT '实际交运时间',
    signed_at         DATETIME     COMMENT '签收时间',
    merchant_id       BIGINT       COMMENT '商家ID',
    version           INT DEFAULT 0 COMMENT '乐观锁',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货单';

ALTER TABLE shipping_order ADD UNIQUE KEY uk_shipping_no (shipping_no);
ALTER TABLE shipping_order ADD UNIQUE KEY uk_order_client_request (order_id, client_request_id);
ALTER TABLE shipping_order ADD UNIQUE KEY uk_provider_tracking (provider_id, tracking_no);
ALTER TABLE shipping_order ADD INDEX idx_order_id (order_id);
ALTER TABLE shipping_order ADD INDEX idx_merchant_id (merchant_id);
ALTER TABLE shipping_order ADD INDEX idx_shipping_status (shipping_status);

CREATE TABLE shipping_order_item (
    id                BIGINT PRIMARY KEY,
    shipping_id       BIGINT NOT NULL COMMENT '发货单ID',
    order_item_id     BIGINT NOT NULL COMMENT '订单明细ID',
    sku_id            BIGINT NOT NULL COMMENT 'SKU ID',
    quantity          INT DEFAULT 0 COMMENT '发货数量',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货明细';

ALTER TABLE shipping_order_item ADD UNIQUE KEY uk_shipping_order_item (shipping_id, order_item_id);
ALTER TABLE shipping_order_item ADD INDEX idx_shipping_id (shipping_id);

CREATE TABLE tracking_record (
    id                BIGINT PRIMARY KEY,
    shipping_id       BIGINT       NOT NULL COMMENT '发货单ID',
    provider_code     VARCHAR(32)  COMMENT '物流公司编码',
    tracking_no       VARCHAR(64)  NOT NULL COMMENT '运单号',
    trace_hash        VARCHAR(64)  NOT NULL COMMENT '轨迹去重哈希',
    trace_time        DATETIME     NOT NULL COMMENT '轨迹时间',
    trace_desc        VARCHAR(512) NOT NULL COMMENT '轨迹描述',
    trace_status      VARCHAR(32)  COMMENT '第三方轨迹状态码',
    event_type        VARCHAR(32)  COMMENT '平台标准事件 PICKED/IN_TRANSIT/DELIVERING/SIGNED/EXCEPTION/RETURNED',
    location          VARCHAR(128) COMMENT '城市/网点',
    raw_data          JSON         COMMENT '原始返回报文',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹';

ALTER TABLE tracking_record ADD UNIQUE KEY uk_shipping_trace (shipping_id, trace_hash);
ALTER TABLE tracking_record ADD INDEX idx_shipping_id (shipping_id);
ALTER TABLE tracking_record ADD INDEX idx_tracking_no (tracking_no);
