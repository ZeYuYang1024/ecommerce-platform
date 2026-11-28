CREATE DATABASE IF NOT EXISTS ecommerce_recommendation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommerce_recommendation;

CREATE TABLE IF NOT EXISTS recommendation_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    spu_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    brand_id BIGINT NULL,
    price DECIMAL(12, 2) NULL,
    event_type VARCHAR(16) NOT NULL,
    request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    surface VARCHAR(16) NULL,
    strategy VARCHAR(32) NULL,
    occurred_at DATETIME(3) NOT NULL,
    lake_batch_id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    exported_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_event (event_id),
    KEY idx_recommendation_user_events (user_id, occurred_at, id),
    KEY idx_recommendation_export (exported_at, lake_batch_id, id)
);

-- Upgrade existing local data without disturbing event IDs or Parquet exports.
SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_event' AND COLUMN_NAME = 'surface'
), 'DO 0', 'ALTER TABLE recommendation_event ADD COLUMN surface VARCHAR(16) NULL');
PREPARE add_column FROM @ddl;
EXECUTE add_column;
DEALLOCATE PREPARE add_column;

SET @ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'recommendation_event' AND COLUMN_NAME = 'strategy'
), 'DO 0', 'ALTER TABLE recommendation_event ADD COLUMN strategy VARCHAR(32) NULL');
PREPARE add_column FROM @ddl;
EXECUTE add_column;
DEALLOCATE PREPARE add_column;

CREATE TABLE IF NOT EXISTS recommendation_user_category (
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    score_7d DOUBLE NOT NULL,
    score_30d DOUBLE NOT NULL,
    PRIMARY KEY (user_id, category_id)
);

CREATE TABLE IF NOT EXISTS recommendation_related_product (
    spu_id BIGINT NOT NULL,
    related_spu_id BIGINT NOT NULL,
    shared_users INT NOT NULL,
    affinity DOUBLE NOT NULL,
    PRIMARY KEY (spu_id, related_spu_id)
);

CREATE TABLE IF NOT EXISTS recommendation_daily_report (
    event_date DATE NOT NULL,
    impressions BIGINT NOT NULL,
    clicks BIGINT NOT NULL,
    click_rate DOUBLE NOT NULL,
    PRIMARY KEY (event_date)
);

CREATE TABLE IF NOT EXISTS recommendation_funnel_report (
    event_date DATE NOT NULL,
    surface VARCHAR(16) NOT NULL,
    strategy VARCHAR(32) NOT NULL,
    impressions BIGINT NOT NULL,
    clicks BIGINT NOT NULL,
    cart_adds BIGINT NOT NULL,
    payments BIGINT NOT NULL,
    PRIMARY KEY (event_date, surface, strategy)
);
