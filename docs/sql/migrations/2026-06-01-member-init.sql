-- 会员积分系统初始化 DDL
-- 数据库: ecommerce_member

CREATE DATABASE IF NOT EXISTS ecommerce_member DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ecommerce_member;

-- 1. 会员等级定义表
CREATE TABLE IF NOT EXISTS member_level (
    id                    BIGINT PRIMARY KEY,
    name                  VARCHAR(32)   NOT NULL COMMENT '等级名称',
    level_code            VARCHAR(16)   NOT NULL UNIQUE COMMENT '编码: REGULAR/SILVER/GOLD/DIAMOND',
    sort_order            INT           NOT NULL COMMENT '排序 1-4',
    growth_threshold      BIGINT        NOT NULL COMMENT '升级所需成长值',
    points_multiplier     DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
    birthday_gift_points  INT           NOT NULL DEFAULT 0 COMMENT '生日礼积分',
    discount_rate         DECIMAL(4,2)  NOT NULL DEFAULT 1.00 COMMENT '专属折扣(预留)',
    free_shipping         TINYINT       NOT NULL DEFAULT 0 COMMENT '包邮(预留)',
    priority_support      TINYINT       NOT NULL DEFAULT 0 COMMENT '优先客服(预留)',
    early_access          TINYINT       NOT NULL DEFAULT 0 COMMENT '新品优先(预留)',
    icon_url              VARCHAR(255) COMMENT '等级图标',
    description           VARCHAR(255) COMMENT '等级描述',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级定义';

-- 2. 用户会员档案表
CREATE TABLE IF NOT EXISTS member_profile (
    id                   BIGINT PRIMARY KEY,
    user_id              BIGINT   NOT NULL UNIQUE COMMENT '关联 auth.user.id',
    level_id             BIGINT   NOT NULL COMMENT '当前等级 FK→member_level.id',
    growth_value         BIGINT   NOT NULL DEFAULT 0 COMMENT '当前成长值',
    total_growth_value   BIGINT   NOT NULL DEFAULT 0 COMMENT '历史累计成长值',
    available_points     BIGINT   NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    total_earned_points  BIGINT   NOT NULL DEFAULT 0 COMMENT '累计获取积分',
    total_spent_points   BIGINT   NOT NULL DEFAULT 0 COMMENT '累计消耗积分',
    version              BIGINT   NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted              TINYINT  NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员档案';

-- 3. 积分流水表
CREATE TABLE IF NOT EXISTS points_transaction (
    id                BIGINT PRIMARY KEY,
    user_id           BIGINT       NOT NULL COMMENT '用户ID',
    direction         VARCHAR(16)  NOT NULL COMMENT 'EARN/SPEND/EXPIRE',
    amount            INT          NOT NULL COMMENT '变动数，正数',
    balance_after     BIGINT       NOT NULL COMMENT '变动后积分余额',
    source_type       VARCHAR(32)  NOT NULL COMMENT 'ORDER/CHECKIN/REVIEW/CAMPAIGN/BIRTHDAY/EXPIRE',
    source_id         VARCHAR(64)  NOT NULL COMMENT '来源业务ID',
    biz_key           VARCHAR(96)  NOT NULL COMMENT '幂等业务键',
    consumed_amount   INT          NOT NULL DEFAULT 0 COMMENT '已被消耗的数量，仅 EARN 使用',
    expire_at         DATETIME     COMMENT '过期时间，仅 EARN 使用',
    remark            VARCHAR(255) COMMENT '备注',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_biz_key (biz_key),
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_expire_at (expire_at),
    INDEX idx_user_direction_expire (user_id, direction, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 4. 成长值流水表
CREATE TABLE IF NOT EXISTS growth_transaction (
    id                BIGINT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    amount            INT          NOT NULL COMMENT '变动数，正数',
    balance_after     BIGINT       NOT NULL COMMENT '变动后成长值',
    source_type       VARCHAR(32)  NOT NULL COMMENT 'ORDER/CHECKIN/REVIEW/CAMPAIGN',
    source_id         VARCHAR(64)  NOT NULL,
    biz_key           VARCHAR(96)  NOT NULL COMMENT '幂等业务键',
    remark            VARCHAR(255),
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_biz_key (biz_key),
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成长值流水';

-- 5. 签到记录表
CREATE TABLE IF NOT EXISTS check_in_record (
    id               BIGINT PRIMARY KEY,
    user_id          BIGINT   NOT NULL,
    check_date       DATE     NOT NULL COMMENT '签到日期',
    consecutive_days INT      NOT NULL COMMENT '连续签到天数',
    points_awarded   INT      NOT NULL COMMENT '本次发放积分',
    biz_key          VARCHAR(96) NOT NULL COMMENT '幂等业务键',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_date (user_id, check_date),
    UNIQUE KEY uk_biz_key (biz_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到记录';
