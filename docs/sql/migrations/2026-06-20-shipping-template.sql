CREATE TABLE shipping_template (
    id                BIGINT PRIMARY KEY,
    template_name     VARCHAR(64)  NOT NULL COMMENT '模板名',
    merchant_id       BIGINT       COMMENT '商家ID，平台模板为NULL',
    calc_type         TINYINT DEFAULT 0 COMMENT '0按件/1按重量/2按体积',
    first_unit        INT DEFAULT 0 COMMENT '首件/首重/首体积',
    first_fee         DECIMAL(10,2) DEFAULT 0 COMMENT '首费',
    continue_unit     INT DEFAULT 0 COMMENT '续件/续重/续体积',
    continue_fee      DECIMAL(10,2) DEFAULT 0 COMMENT '续费',
    free_condition    JSON         COMMENT '包邮条件 {"type":"amount","threshold":99}',
    region_rules      JSON         COMMENT '区域规则',
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           INT DEFAULT 0,
    INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费模板';
