# 会员等级与积分系统 — 技术方案

> 状态: Review | 日期: 2026-06-01

## 1. 概述

### 1.1 目标

构建全功能会员生态：成长值驱动的等级体系 + 多渠道积分获取与消耗。

### 1.2 核心决策汇总

| 决策项 | 结论 |
|--------|------|
| 模块归属 | 新建 `ecommerce-member` 微服务 |
| 等级判定 | 成长值制（与积分分离，只增不减） |
| 等级档位 | 4 级：普通→银卡→金卡→钻石 |
| 积分获取 | 下单 + 签到 + 评价 + 营销活动 |
| 积分消耗 | 下单抵扣 + 商城兑换 + 抽奖 |
| 积分有效期 | 滚动有效期，获取后 12 个月过期 |
| 集成方式 | MQ 事件驱动（RocketMQ + Outbox） |
| 等级权益 | 积分倍率、折扣、包邮、生日礼、优先客服、新品优先 |

---

## 2. 数据库设计

数据库: `ecommerce_member`

### 2.1 member_level — 会员等级定义

```sql
CREATE TABLE member_level (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(32)  NOT NULL COMMENT '等级名称',
    level_code      VARCHAR(16)  NOT NULL UNIQUE COMMENT '编码: REGULAR/SILVER/GOLD/DIAMOND',
    sort_order      INT          NOT NULL COMMENT '排序 1-4',
    growth_threshold BIGINT      NOT NULL COMMENT '升级所需成长值',
    points_multiplier DECIMAL(3,1) NOT NULL DEFAULT 1.0 COMMENT '积分倍率',
    discount_rate   DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '专属折扣',
    free_shipping   TINYINT      NOT NULL DEFAULT 0 COMMENT '包邮: 0/1',
    birthday_gift_points INT     NOT NULL DEFAULT 0 COMMENT '生日礼积分',
    priority_support TINYINT     NOT NULL DEFAULT 0 COMMENT '优先客服: 0/1',
    early_access    TINYINT      NOT NULL DEFAULT 0 COMMENT '新品优先: 0/1',
    icon_url        VARCHAR(255) COMMENT '等级图标',
    description     VARCHAR(255) COMMENT '等级描述',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT      NOT NULL DEFAULT 0
);
```

**种子数据：**

| level_code | name | sort | threshold | multiplier | discount | free_shipping | birthday_gift |
|------------|------|------|-----------|------------|----------|---------------|---------------|
| REGULAR | 普通会员 | 1 | 0 | 1.0 | 1.00 | 0 | 0 |
| SILVER | 银卡会员 | 2 | 1000 | 1.2 | 0.98 | 0 | 50 |
| GOLD | 金卡会员 | 3 | 5000 | 1.5 | 0.95 | 1 | 100 |
| DIAMOND | 钻石会员 | 4 | 20000 | 2.0 | 0.90 | 1 | 200 |

### 2.2 member_profile — 用户会员档案

```sql
CREATE TABLE member_profile (
    id                  BIGINT PRIMARY KEY,
    user_id             BIGINT    NOT NULL UNIQUE COMMENT '关联 auth.user.id',
    level_id            BIGINT    NOT NULL COMMENT '当前等级 FK→member_level.id',
    growth_value        BIGINT    NOT NULL DEFAULT 0 COMMENT '当前成长值',
    total_growth_value  BIGINT    NOT NULL DEFAULT 0 COMMENT '历史累计成长值(降级保护)',
    available_points    BIGINT    NOT NULL DEFAULT 0 COMMENT '可用积分(不含锁定)',
    locked_points       BIGINT    NOT NULL DEFAULT 0 COMMENT '锁定中的积分(下单预扣)',
    total_earned_points BIGINT    NOT NULL DEFAULT 0 COMMENT '累计获取',
    total_spent_points  BIGINT    NOT NULL DEFAULT 0 COMMENT '累计消耗',
    created_at          DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT   NOT NULL DEFAULT 0
);
```

- 用户注册时**不**自动创建 profile，首次获取积分/签到触发创建
- `available_points` 通过流水表 SUM 校验一致性
- 等级变更时同步更新 `level_id`

### 2.3 points_transaction — 积分流水

```sql
CREATE TABLE points_transaction (
    id            BIGINT PRIMARY KEY,
    user_id       BIGINT      NOT NULL COMMENT '用户ID',
    direction     VARCHAR(8)  NOT NULL COMMENT 'EARN/SPEND/EXPIRE',
    amount        INT         NOT NULL COMMENT '变动数(正数)',
    balance_after BIGINT      NOT NULL COMMENT '变动后余额',
    source_type   VARCHAR(32) NOT NULL COMMENT 'ORDER/CHECKIN/REVIEW/CAMPAIGN/REDEEM/LOTTERY/EXPIRE',
    source_id     VARCHAR(64) COMMENT '来源业务ID',
    expire_at     DATETIME    COMMENT '过期时间(EARN类型)',
    remark        VARCHAR(255) COMMENT '备注',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_expire_at (expire_at)
);
```

- Append-only，不允许修改删除
- 消费积分时按 FIFO 从最早的未过期批次扣除

### 2.4 growth_transaction — 成长值流水

```sql
CREATE TABLE growth_transaction (
    id            BIGINT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    amount        INT         NOT NULL COMMENT '变动数',
    balance_after BIGINT      NOT NULL COMMENT '变动后余额',
    source_type   VARCHAR(32) NOT NULL COMMENT 'ORDER/CHECKIN/REVIEW/CAMPAIGN/LEVEL_DOWNGRADE/LEVEL_DEDUCT',
    source_id     VARCHAR(64),
    remark        VARCHAR(255),
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, created_at)
);
```

- 通常只增不减，降级时扣减当前成长值

### 2.5 check_in_record — 签到记录

```sql
CREATE TABLE check_in_record (
    id               BIGINT PRIMARY KEY,
    user_id          BIGINT  NOT NULL,
    check_date       DATE    NOT NULL COMMENT '签到日期',
    consecutive_days INT     NOT NULL COMMENT '连续签到天数',
    points_awarded   INT     NOT NULL COMMENT '本次获得积分',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_user_date (user_id, check_date)
);
```

---

## 3. 模块结构

```
ecommerce-member/
└── src/main/java/com/ecommerce/member/
    ├── MemberApplication.java
    ├── common/
    │   └── MemberErrorCode.java          # 错误码枚举 (1002xxxx)
    ├── entity/
    │   ├── MemberLevel.java              # Entity → member_level
    │   ├── MemberProfile.java            # Entity → member_profile
    │   ├── PointsTransaction.java        # Entity → points_transaction
    │   ├── GrowthTransaction.java        # Entity → growth_transaction
    │   └── CheckInRecord.java            # Entity → check_in_record
    ├── mapper/
    │   ├── MemberLevelMapper.java
    │   ├── MemberProfileMapper.java
    │   ├── PointsTransactionMapper.java
    │   ├── GrowthTransactionMapper.java
    │   └── CheckInRecordMapper.java
    ├── dto/
    │   ├── request/
    │   │   ├── PointsGrantRequest.java
    │   │   ├── PointsDeductRequest.java
    │   │   └── LevelUpdateRequest.java
    │   └── response/
    │       ├── MemberProfileVO.java
    │       ├── PointsTransactionVO.java
    │       ├── GrowthTransactionVO.java
    │       ├── CheckInStatusVO.java
    │       └── MemberLevelVO.java
    ├── service/
    │   ├── MemberService.java            # 接口
    │   ├── PointsService.java            # 接口
    │   ├── GrowthService.java            # 接口
    │   ├── CheckInService.java           # 接口
    │   └── impl/
    │       ├── MemberServiceImpl.java
    │       ├── PointsServiceImpl.java    # 积分核心：获取/消耗/过期/FIFO
    │       ├── GrowthServiceImpl.java    # 成长值核心：增减+升降级
    │       ├── CheckInServiceImpl.java   # 签到逻辑+连续天数
    │       └── PointsExpiryServiceImpl.java # @Scheduled 过期扫描
    ├── controller/
    │   ├── MemberController.java         # C端 /api/v1/member/*
    │   └── admin/
    │       └── MemberAdminController.java # 后台 /api/v1/admin/member/*
    └── consumer/
        ├── OrderPaidConsumer.java        # 订单支付 → 积分+成长值
        └── ReviewCreatedConsumer.java    # 评价完成 → 积分
```

### 依赖

- `ecommerce-common` (BaseEntity, Result, ErrorCode, JwtUtils)
- MyBatis-Plus
- RocketMQ Spring Boot Starter
- Spring Boot Validation

---

## 4. API 设计

### 4.1 C 端接口 (Gateway 路由: /api/v1/member/**)

#### GET /api/v1/member/profile

获取当前用户会员信息。

```json
// Response
{
  "code": 200, "message": "success",
  "data": {
    "level": { "name": "金卡会员", "levelCode": "GOLD", "sortOrder": 3, "iconUrl": "..." },
    "growthValue": 6230,
    "nextLevelGrowth": 20000,
    "availablePoints": 1580,
    "totalEarnedPoints": 3200,
    "levelBenefits": {
      "pointsMultiplier": 1.5,
      "discountRate": 0.95,
      "freeShipping": true,
      "prioritySupport": true,
      "earlyAccess": false
    }
  }
}
```

#### GET /api/v1/member/points/transactions?page=1&size=20

积分流水（分页）。

#### GET /api/v1/member/growth/transactions?page=1&size=20

成长值流水（分页）。

#### POST /api/v1/member/check-in

```json
// Response
{
  "code": 200,
  "data": {
    "consecutiveDays": 5,
    "pointsAwarded": 1,
    "bonusPoints": 0,
    "totalPoints": 5
  }
}
```

**签到规则**：
- 每日一次，(user_id, check_date) 唯一约束
- 连续 7 天额外 +5 积分
- 中断后连续天数重置

#### GET /api/v1/member/check-in/status

```json
{ "checkedToday": true, "consecutiveDays": 5 }
```

### 4.2 后台接口 (Gateway 路由: /api/v1/admin/member/**)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /levels | 等级列表 |
| PUT | /levels/{id} | 更新等级配置 |
| GET | /profiles?page=&size=&levelCode=&keyword= | 用户会员列表 |
| GET | /profiles/{userId} | 用户会员详情 |
| GET | /points/transactions?page=&size=&userId=&sourceType= | 全部积分流水 |
| POST | /points/grant | 手动发放积分 (营销活动) |
| POST | /points/deduct | 手动扣减积分 |

---

## 5. 事件集成

### 5.1 订单支付成功 → 发放积分+成长值

**Topic**: `order-paid`
**Consumer**: `OrderPaidConsumer`

```json
{
  "orderId": "123456",
  "userId": "789",
  "paidAmount": 19900,    // 分
  "orderNo": "ORD20260601001"
}
```

处理逻辑：
1. 计算积分 = floor(paidAmount / 100) × level.pointsMultiplier
2. 计算成长值 = floor(paidAmount / 100)
3. 调用 PointsService.earn(userId, amount, ORDER, orderId, 12个月后)
4. 调用 GrowthService.add(userId, amount, ORDER, orderId)
5. 调用 MemberService.checkLevelUpgrade(userId)

### 5.2 评价完成 → 发放积分

**Topic**: `review-created`
**Consumer**: `ReviewCreatedConsumer`

```json
{
  "reviewId": "456",
  "userId": "789",
  "orderId": "123456",
  "reviewType": "IMAGE"  // TEXT/IMAGE/VIDEO
}
```

处理逻辑：
- TEXT → +5 积分
- IMAGE → +10 积分
- VIDEO → +15 积分
- 同一订单仅发放一次（通过 orderId 去重）

### 5.3 需要其他模块配合的事件发布

| 事件 | 发布方 | Topic | 需要修改 |
|------|--------|-------|----------|
| 订单支付成功 | ecommerce-order | order-paid | order 模块已有支付回调，需加 Outbox 发布 |
| 评价创建 | ecommerce-product | review-created | product 模块评价接口需加事件发布 |

### 5.4 下单积分抵扣（同步，两阶段提交）

下单时，order 模块通过 HTTP 调用 member 服务：

| 阶段 | API | 说明 |
|------|-----|------|
| 提交订单 | `POST /api/v1/member/points/pre-deduct` | 预扣积分（锁定），返回流水 ID |
| 支付成功 | `POST /api/v1/member/points/confirm-deduct` | 确认扣除，流水从 LOCKED → SPEND |
| 支付失败/取消 | `POST /api/v1/member/points/cancel-deduct` | 释放锁定，流水从 LOCKED → 删除 |

**锁定机制**：`member_profile` 表增加 `locked_points` 字段：
- `available_points` — 可用积分（不含锁定）
- `locked_points` — 锁定中的积分
- 预扣时: available_points -= N, locked_points += N
- 确认时: locked_points -= N, total_spent_points += N
- 取消时: locked_points -= N, available_points += N
- 超时未确认的锁定积分，定时任务 30 分钟后自动释放

---

## 6. 核心业务逻辑

### 6.1 积分消费 FIFO 算法

```
消费 N 积分时：
1. 查询未过期、未被消耗完的 EARN 流水，按 expire_at ASC 排序
2. 依次从最早的批次扣除，直到满足 N
3. 更新每笔流水的已消耗量（或标记消耗完）
4. 插入 SPEND 流水记录
```

**优化**：在 `points_transaction` 表增加 `consumed_amount` 字段追踪已消耗量，用 `amount - consumed_amount > 0` 作为可用批次的筛选条件。

### 6.2 升级判定

```
每次成长值增加后：
1. 查询所有等级按 sort_order ASC
2. 找到 growth_threshold 最大且 ≤ 当前成长值的等级
3. 如果等级高于当前等级 → 升级
4. 更新 member_profile.level_id
```

### 6.3 降级策略

每年 12 月 31 日执行（或可配置周期）：
1. 查询所有 `level_id != REGULAR` 的用户
2. 判断 `total_growth_value` 是否满足当前等级阈值
3. 不满足 → 降级到满足阈值的最高等级
4. 扣除成长值（差额），记录流水
5. 给予 30 天保护期：降级后 30 天内恢复成长值可恢复等级

### 6.4 积分过期

`@Scheduled(cron = "0 0 2 * * ?")` 每日凌晨 2:00：
1. 查询所有 `expire_at < NOW()` 且 `amount - consumed_amount > 0` 的 EARN 记录
2. 批量插入 EXPIRE 流水
3. 扣减 `member_profile.available_points`
4. 标记原 EARN 记录的 consumed_amount = amount

### 6.5 生日礼

`@Scheduled(cron = "0 0 10 * * ?")` 每日 10:00：
1. 查询当月生日的用户（需用户表有 birthday 字段）
2. 如果已发放过 → 跳过（按月记录去重）
3. 按等级对应的 birthday_gift_points 发放积分

---

## 7. 前端改造

### 7.1 管理后台 (ecommerce-admin) — 新增页面

| 页面 | 路由 | 内容 |
|------|------|------|
| 会员等级管理 | /member/levels | 等级列表 + 编辑权益（门槛/倍率/折扣/开关） |
| 用户会员列表 | /member/profiles | 表格: 用户名/等级/成长值/积分 + 筛选 |
| 积分流水 | /member/points | 全量流水 + user/sourceType/时间筛选 |
| 积分发放 | /member/points/grant | 表单: 用户ID + 积分数量 + 原因 |

### 7.2 商城端 (ecommerce-web)

用户中心增加：
- 会员卡片（等级图标 + 成长值进度条 + 距下一级差）
- 积分余额 + 积分流水入口
- 签到按钮

### 7.3 小程序端 (ecommerce-miniprogram)

用户页面增加：
- 会员等级展示（图标 + 名称 + 成长值进度）
- 积分余额入口
- 签到入口（日历视图）

---

## 8. 错误码

| 错误码 | 枚举 | 说明 |
|--------|------|------|
| 10020001 | MEMBER_PROFILE_NOT_FOUND | 会员档案不存在 |
| 10020002 | INSUFFICIENT_POINTS | 积分不足 |
| 10020003 | ALREADY_CHECKED_IN | 今日已签到 |
| 10020004 | POINTS_RULE_NOT_FOUND | 积分规则不存在 |
| 10020005 | LEVEL_UPGRADE_FAILED | 等级升级失败 |
| 10020006 | INVALID_POINTS_AMOUNT | 积分数量无效 |
| 10020007 | DUPLICATE_SOURCE | 重复来源（防重） |

---

## 9. 外部模块依赖

实现本方案需要对以下现有模块做小幅修改：

| 模块 | 修改项 | 说明 |
|------|--------|------|
| ecommerce-auth | User 表增加 `birthday DATE` 字段 | 生日礼功能依赖（可选，不加则跳过生日礼） |
| ecommerce-order | 支付成功回调处发布 `order-paid` MQ 消息 | 已有 Outbox 机制，新增一个 Topic |
| ecommerce-order | 下单时调用 member 预扣积分 API | 结算页积分抵扣功能 |
| ecommerce-product | 评价创建后发布 `review-created` MQ 消息 | 评价得积分功能 |
| ecommerce-gateway | 路由配置增加 `/api/v1/member/**` 和 `/api/v1/admin/member/**` | 网关转发 |

**注意**：
- 积分抵扣为可选功能：如果 order 模块不改，积分只能用于商城兑换和抽奖
- 生日礼为可选功能：如果 User 表不增加 birthday 字段，跳过生日礼定时任务
- 建议 Phase 1-2 先将 member 模块独立完成，Phase 3-4 再逐步接入各外部事件

---

## 10. 实施顺序建议

| 阶段 | 内容 | 预估 |
|------|------|------|
| Phase 1 | 数据库 DDL + 模块搭建 + 等级定义 + 档案查询 | 基础骨架 |
| Phase 2 | 积分核心 (earn/spend/expire) + 成长值核心 + 等级升降 | 核心逻辑 |
| Phase 3 | 订单支付消费者 + 签到功能 | 接入订单 |
| Phase 4 | 评价消费者 + 营销活动手动发放 | 接入评价 |
| Phase 5 | 管理后台页面 | 运营工具 |
| Phase 6 | 商城端 + 小程序端展示 | 用户体验 |
