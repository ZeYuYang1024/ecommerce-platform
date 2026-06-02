USE ecommerce_member;

CREATE TABLE IF NOT EXISTS points_reservation (
    id BIGINT PRIMARY KEY,
    reservation_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    scene_type VARCHAR(32) NOT NULL,
    reserved_points INT NOT NULL,
    consumed_points INT NOT NULL DEFAULT 0,
    released_points INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(96) NOT NULL,
    expired_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_reservation_no (reservation_no),
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    UNIQUE KEY uk_order_scene (order_no, scene_type),
    KEY idx_user_status (user_id, status),
    KEY idx_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分预占主记录';

CREATE TABLE IF NOT EXISTS points_consume_detail (
    id BIGINT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    earn_tx_id BIGINT NOT NULL,
    consume_points INT NOT NULL,
    restored_points INT NOT NULL DEFAULT 0,
    expire_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_reservation_id (reservation_id),
    KEY idx_user_expire_at (user_id, expire_at),
    KEY idx_earn_tx_id (earn_tx_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分消费批次明细';
