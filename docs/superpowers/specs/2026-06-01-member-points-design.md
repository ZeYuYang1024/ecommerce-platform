# 会员等级与积分系统 — 技术方案

> 状态: Review | 日期: 2026-06-01

## 1. 概述

### 1.1 目标

构建一个可独立演进的会员中心，先落地以下核心能力：

- 成长值驱动的会员等级体系
- 订单支付后自动发放积分与成长值
- 每日签到积分
- 评价完成积分
- 会员档案与积分/成长值流水查询
- 运营后台等级配置与手动发放积分

本方案以当前仓库已有微服务边界、RocketMQ/Outbox 基础设施、订单/支付/评价数据模型为约束，优先保证第一阶段能稳定落地。

### 1.2 本期核心决策

| 决策项 | 结论 |
|--------|------|
| 模块归属 | 新建 `ecommerce-member` 微服务 |
| 等级判定 | 成长值制，等级由当前成长值决定 |
| 等级档位 | 4 级：普通→银卡→金卡→钻石 |
| 积分获取 | 下单支付 + 签到 + 评价 + 后台手动发放 |
| 积分消耗 | 本期不做订单抵扣，仅保留后续扩展位 |
| 积分有效期 | 获取后 12 个月过期 |
| 集成方式 | 消费现有 MQ 事件 + 必要的服务间 HTTP 查询 |
| 等级权益 | 本期先实现积分倍率、生日礼积分配置；折扣/包邮/新品优先先做配置展示，不接结算链路 |

### 1.3 明确不在本期范围内

以下能力暂不进入本期实现，避免和现有订单/支付链路产生额外耦合：

- 下单积分预扣、确认扣减、取消回退
- 会员折扣直接参与订单结算
- 会员包邮直接参与运费计算
- 年度降级、保级周期、降级保护期
- 视频评价积分
- 商城兑换、抽奖等积分消费场景

这些能力待 member 模块稳定后，再单独出二期方案。

---

## 2. 现有系统约束

### 2.1 支付成功事件已存在，不能改动既有契约

当前仓库中：

- `ecommerce-payment` 已通过 Outbox 发布 `order-paid`
- `ecommerce-order` 已消费 `order-paid` 做订单状态流转
- 公共消息体已定义为 `com.ecommerce.common.dto.OrderPaidMessage`

因此 member 模块不能直接重定义 `order-paid` topic 或 payload。会员积分发放应复用现有 `order-paid` 事件，再通过订单内部查询接口补齐业务字段。

### 2.2 订单内部查询能力现状

当前 `ecommerce-order` 已提供：

- `GET /api/v1/internal/orders/no/{orderNo}?userId=...`
- 返回 `OrderInternalVO { id, orderNo, totalAmount, status }`

因此 member 在消费 `order-paid` 后，可基于 `orderNo + userId` 查询订单金额，但如需完全避免追加查询，可在后续扩展新的会员专用支付事件。

### 2.3 评价模型现状

当前 `review` 表已有：

- `order_id`
- `content`
- `images`

当前没有 `reviewType`、`video` 字段。因此本期评价积分规则只能基于“纯文字/带图”两档，不定义视频奖励。

### 2.4 用户域现状

用户基础信息当前由 `ecommerce-auth.user` 承载。若要支持生日礼，需要在该表补充 `birthday DATE` 字段，并同步更新用户资料查询/编辑接口。

---

## 3. 数据库设计

数据库: `ecommerce_member`

### 3.1 member_level — 会员等级定义

```sql
CREATE TABLE member_level (
    id                    BIGINT PRIMARY KEY,
    name                  VARCHAR(32)   NOT NULL COMMENT '等级名称',
    level_code            VARCHAR(16)   NOT NULL UNIQUE COMMENT '编码: REGULAR/SILVER/GOLD/DIAMOND',
    sort_order            INT           NOT NULL COMMENT '排序 1-4',
    growth_threshold      BIGINT        NOT NULL COMMENT '升级所需成长值',
    points_multiplier     DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '积分倍率',
    birthday_gift_points  INT           NOT NULL DEFAULT 0 COMMENT '生日礼积分',
    discount_rate         DECIMAL(4,2) NOT NULL DEFAULT 1.00 COMMENT '专属折扣(预留)',
    free_shipping         TINYINT       NOT NULL DEFAULT 0 COMMENT '包邮(预留)',
    priority_support      TINYINT       NOT NULL DEFAULT 0 COMMENT '优先客服(预留)',
    early_access          TINYINT       NOT NULL DEFAULT 0 COMMENT '新品优先(预留)',
    icon_url              VARCHAR(255) COMMENT '等级图标',
    description           VARCHAR(255) COMMENT '等级描述',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT       NOT NULL DEFAULT 0
);
```

说明：

- `discount_rate/free_shipping/priority_support/early_access` 本期只做等级配置和前端展示，不参与订单计算。
- `points_multiplier` 保留两位小数，避免后续倍率策略受限。

**种子数据：**

| level_code | name | sort | threshold | multiplier | birthday_gift |
|------------|------|------|-----------|------------|---------------|
| REGULAR | 普通会员 | 1 | 0 | 1.00 | 0 |
| SILVER | 银卡会员 | 2 | 1000 | 1.20 | 50 |
| GOLD | 金卡会员 | 3 | 5000 | 1.50 | 100 |
| DIAMOND | 钻石会员 | 4 | 20000 | 2.00 | 200 |

### 3.2 member_profile — 用户会员档案

```sql
CREATE TABLE member_profile (
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
);
```

说明：

- 本期不做积分预扣，因此移除 `locked_points`。
- 用户注册时不强制创建 profile；`GET /member/profile` 需支持“无档案时返回默认 REGULAR 视图”，首次积分/成长值变动时再落库。
- `version` 用于并发更新积分余额/成长值时的乐观锁控制。

### 3.3 points_transaction — 积分流水

```sql
CREATE TABLE points_transaction (
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
);
```

说明：

- 流水表采用 append-only 模型，不允许物理删除。
- 通过 `biz_key` 做幂等防重，例如：
  - 订单积分：`ORDER:{orderNo}:EARN`
  - 签到积分：`CHECKIN:{userId}:{yyyyMMdd}`
  - 评价积分：`REVIEW:{reviewId}:EARN`
  - 生日礼：`BIRTHDAY:{userId}:{yyyyMM}`
- 本期没有积分消费场景，但保留 `SPEND/consumed_amount` 设计，为二期消费能力铺底。

### 3.4 growth_transaction — 成长值流水

```sql
CREATE TABLE growth_transaction (
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
);
```

说明：

- 本期成长值只增不减，不实现降级扣减。
- 成长值幂等键和积分类似，例如 `ORDER:{orderNo}:GROWTH`。

### 3.5 check_in_record — 签到记录

```sql
CREATE TABLE check_in_record (
    id               BIGINT PRIMARY KEY,
    user_id          BIGINT   NOT NULL,
    check_date       DATE     NOT NULL COMMENT '签到日期',
    consecutive_days INT      NOT NULL COMMENT '连续签到天数',
    points_awarded   INT      NOT NULL COMMENT '本次发放积分',
    biz_key          VARCHAR(96) NOT NULL COMMENT '幂等业务键',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_user_date (user_id, check_date),
    UNIQUE KEY uk_biz_key (biz_key)
);
```

---

## 4. 模块结构

```
ecommerce-member/
└── src/main/java/com/ecommerce/member/
    ├── MemberApplication.java
    ├── client/
    │   ├── OrderClient.java              # 调用 ecommerce-order 内部接口
    │   └── AuthClient.java               # 可选，查询用户资料/生日
    ├── common/
    │   └── MemberErrorCode.java          # 错误码枚举 (1002xxxx)
    ├── entity/
    │   ├── MemberLevel.java
    │   ├── MemberProfile.java
    │   ├── PointsTransaction.java
    │   ├── GrowthTransaction.java
    │   └── CheckInRecord.java
    ├── mapper/
    │   ├── MemberLevelMapper.java
    │   ├── MemberProfileMapper.java
    │   ├── PointsTransactionMapper.java
    │   ├── GrowthTransactionMapper.java
    │   └── CheckInRecordMapper.java
    ├── dto/
    │   ├── request/
    │   │   ├── PointsGrantRequest.java
    │   │   └── MemberLevelUpdateRequest.java
    │   └── response/
    │       ├── MemberProfileVO.java
    │       ├── PointsTransactionVO.java
    │       ├── GrowthTransactionVO.java
    │       ├── CheckInStatusVO.java
    │       └── MemberLevelVO.java
    ├── service/
    │   ├── MemberService.java
    │   ├── PointsService.java
    │   ├── GrowthService.java
    │   ├── CheckInService.java
    │   └── impl/
    │       ├── MemberServiceImpl.java
    │       ├── PointsServiceImpl.java
    │       ├── GrowthServiceImpl.java
    │       ├── CheckInServiceImpl.java
    │       ├── MemberProfileFactory.java      # 无档案时创建默认档案
    │       └── PointsExpiryServiceImpl.java
    ├── controller/
    │   ├── MemberController.java
    │   └── admin/
    │       └── MemberAdminController.java
    └── consumer/
        ├── OrderPaidConsumer.java
        └── ReviewCreatedConsumer.java
```

### 依赖

- `ecommerce-common` (BaseEntity, Result, ErrorCode, DTO)
- MyBatis-Plus
- RocketMQ Spring Boot Starter
- Spring Boot Validation
- OpenFeign

---

## 5. API 设计

### 5.1 C 端接口 (Gateway 路由: /api/v1/member/**)

#### GET /api/v1/member/profile

获取当前用户会员信息。

说明：

- 如果 `member_profile` 尚未创建，返回默认 `REGULAR` 等级视图，积分和成长值均为 0。
- 响应中应明确 `hasProfile`，便于前端判断是否为默认视图。

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "hasProfile": true,
    "level": {
      "name": "金卡会员",
      "levelCode": "GOLD",
      "sortOrder": 3,
      "iconUrl": "..."
    },
    "growthValue": 6230,
    "nextLevelGrowth": 20000,
    "availablePoints": 1580,
    "totalEarnedPoints": 3200,
    "levelBenefits": {
      "pointsMultiplier": 1.5,
      "birthdayGiftPoints": 100,
      "discountRate": 0.95,
      "freeShipping": true,
      "prioritySupport": true,
      "earlyAccess": false
    }
  }
}
```

#### GET /api/v1/member/points/transactions?page=1&size=20

积分流水分页。

#### GET /api/v1/member/growth/transactions?page=1&size=20

成长值流水分页。

#### POST /api/v1/member/check-in

```json
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

**签到规则：**

- 每日一次，`(user_id, check_date)` 唯一约束
- 基础奖励 1 积分
- 连续 7 天额外 +5 积分
- 中断后连续天数重置
- 本期签到不发成长值

#### GET /api/v1/member/check-in/status

```json
{
  "checkedToday": true,
  "consecutiveDays": 5
}
```

### 5.2 后台接口 (Gateway 路由: /api/v1/admin/member/**)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /levels | 等级列表 |
| PUT | /levels/{id} | 更新等级配置 |
| GET | /profiles?page=&size=&levelCode=&keyword= | 用户会员列表 |
| GET | /profiles/{userId} | 用户会员详情 |
| GET | /points/transactions?page=&size=&userId=&sourceType= | 全部积分流水 |
| POST | /points/grant | 手动发放积分 |

说明：

- 本期不开放后台手动扣减积分，避免在没有完整消费模型前引入余额一致性问题。

---

## 6. 事件集成

### 6.1 订单支付成功 -> 发放积分 + 成长值

**Topic**: `order-paid`  
**发布方**: `ecommerce-payment`  
**Consumer**: `ecommerce-member.consumer.OrderPaidConsumer`

复用现有公共消息体：

```json
{
  "orderNo": "2026060112340001",
  "status": 1,
  "paidAt": "2026-06-01 12:34:56",
  "transactionId": "txn-123",
  "idempotencyKey": "payment-paid:2026060112340001",
  "errorMessage": null
}
```

处理逻辑：

1. 仅处理 `status = 1` 的支付成功消息
2. 通过订单内部接口按 `orderNo + userId` 查询订单信息；如果现有消息无法直接提供 `userId`，则新增 member 专用订单内部查询接口，按 `orderNo` 返回 `userId + totalAmount + status`
3. 计算基础积分 `basePoints = floor(totalAmount / 100)`
4. 按当前会员等级倍率计算实发积分 `earnedPoints = floor(basePoints * pointsMultiplier)`
5. 发放积分，`bizKey = ORDER:{orderNo}:EARN`
6. 发放成长值，`growth = basePoints`，`bizKey = ORDER:{orderNo}:GROWTH`
7. 重新判定会员等级

说明：

- 订单退款消息目前也复用 `order-paid` 且 `status = 5`，本期 member 忽略退款回滚，不做积分追回。
- 是否需要退款扣回积分，后续单独设计。

### 6.2 评价完成 -> 发放积分

**Topic**: `review-created`  
**发布方**: `ecommerce-product`  
**Consumer**: `ecommerce-member.consumer.ReviewCreatedConsumer`

建议新增公共消息体：

```json
{
  "reviewId": "456",
  "userId": "789",
  "orderId": "123456",
  "hasImages": true
}
```

处理逻辑：

- `hasImages = false` -> +5 积分
- `hasImages = true` -> +10 积分
- 幂等键：`REVIEW:{reviewId}:EARN`
- 同一 `reviewId` 只发一次

说明：

- 本期不定义 `TEXT/IMAGE/VIDEO` 枚举，避免与现有 `review` 表结构不一致。
- `ecommerce-product` 在评价创建成功后发布该事件。

### 6.3 其他模块配合项

| 模块 | 修改项 | 说明 |
|------|--------|------|
| ecommerce-payment | 保持现有 `order-paid` 发布契约不变 | member 直接消费现有 topic |
| ecommerce-order | 提供 member 可用的内部查询接口 | 至少返回 `orderNo/userId/totalAmount/status` |
| ecommerce-product | 评价创建后发布 `review-created` MQ 消息 | payload 贴合现有 `review` 模型 |
| ecommerce-auth | `user` 表新增 `birthday DATE` 字段 | 用于生日礼，可选 |
| ecommerce-gateway | 路由增加 `/api/v1/member/**`、`/api/v1/admin/member/**` | 网关转发 |

---

## 7. 核心业务逻辑

### 7.1 积分发放与幂等

所有自动积分发放都遵循以下步骤：

1. 基于 `biz_key` 查重
2. 不存在则创建/加载 `member_profile`
3. 通过乐观锁更新 `available_points`、`total_earned_points`
4. 插入积分流水
5. 返回成功

如果 `biz_key` 冲突，视为已处理成功，不重复发放。

### 7.2 积分过期

`@Scheduled(cron = "0 0 2 * * ?")` 每日凌晨 2:00：

1. 查询所有 `direction = EARN` 且 `expire_at < NOW()` 且 `amount - consumed_amount > 0` 的记录
2. 对剩余未消费积分插入 `EXPIRE` 流水
3. 扣减 `member_profile.available_points`
4. 将原 `EARN` 记录的 `consumed_amount` 更新为 `amount`

说明：

- 虽然本期没有积分消费能力，但仍保留 `consumed_amount`，这样后续加入消费时不需要重做 DDL。

### 7.3 升级判定

每次成长值增加后：

1. 查询所有等级并按 `sort_order ASC`
2. 找到 `growth_threshold <= currentGrowthValue` 的最高等级
3. 若高于当前等级则升级
4. 更新 `member_profile.level_id`

说明：

- 本期不实现自动降级。
- `total_growth_value` 仅用于累计统计和未来活动玩法，不参与本期等级回退判断。

### 7.4 生日礼

`@Scheduled(cron = "0 0 10 * * ?")` 每日 10:00：

1. 查询当天生日的用户（按月日匹配，不是按整月）
2. 判断当月是否已发放过生日礼
3. 按用户当前等级对应的 `birthday_gift_points` 发放积分
4. 幂等键：`BIRTHDAY:{userId}:{yyyyMM}`

说明：

- 如果 `ecommerce-auth.user` 未增加 `birthday` 字段，则本任务不启用。

### 7.5 签到

签到成功后：

1. 计算连续签到天数
2. 计算积分奖励
3. 生成 `CHECKIN:{userId}:{yyyyMMdd}` 幂等键
4. 发放积分并插入签到记录

---

## 8. 前端改造

### 8.1 管理后台 (ecommerce-admin)

| 页面 | 路由 | 内容 |
|------|------|------|
| 会员等级管理 | /member/levels | 等级列表 + 编辑倍率/生日礼/展示型权益 |
| 用户会员列表 | /member/profiles | 表格：用户名/等级/成长值/积分 + 筛选 |
| 积分流水 | /member/points | 全量流水 + user/sourceType/时间筛选 |
| 积分发放 | /member/points/grant | 表单：用户ID + 积分数量 + 原因 |

### 8.2 商城端 (ecommerce-web)

用户中心增加：

- 会员卡片（等级图标 + 成长值进度条 + 距下一级差值）
- 当前积分余额
- 积分流水入口
- 签到按钮

### 8.3 小程序端 (ecommerce-miniprogram)

用户页面增加：

- 会员等级展示（图标 + 名称 + 成长值进度）
- 积分余额入口
- 签到入口

---

## 9. 错误码

| 错误码 | 枚举 | 说明 |
|--------|------|------|
| 10020001 | MEMBER_PROFILE_NOT_FOUND | 会员档案不存在 |
| 10020002 | INSUFFICIENT_POINTS | 积分不足 |
| 10020003 | ALREADY_CHECKED_IN | 今日已签到 |
| 10020004 | LEVEL_NOT_FOUND | 会员等级不存在 |
| 10020005 | LEVEL_UPGRADE_FAILED | 等级升级失败 |
| 10020006 | INVALID_POINTS_AMOUNT | 积分数量无效 |
| 10020007 | DUPLICATE_SOURCE | 重复来源 |
| 10020008 | ORDER_INFO_NOT_FOUND | 订单信息不存在 |
| 10020009 | UNSUPPORTED_EVENT_STATUS | 不支持的事件状态 |

说明：

- `INSUFFICIENT_POINTS` 本期主要为后续预留，本期可能仅用于后台校验或未来扩展。

---

## 10. 实施顺序建议

| 阶段 | 内容 | 说明 |
|------|------|------|
| Phase 1 | DDL + 模块搭建 + 等级种子数据 + 档案/流水查询 | 建基础骨架 |
| Phase 2 | 积分发放核心 + 成长值核心 + 升级逻辑 + 幂等控制 | 建稳定内核 |
| Phase 3 | 接入 `order-paid` + 订单内部查询接口 | 打通支付得积分 |
| Phase 4 | 签到功能 + 积分过期任务 | 补齐用户侧能力 |
| Phase 5 | 接入 `review-created` + 后台手动发放积分 | 扩展来源 |
| Phase 6 | 管理后台 + web/miniprogram 展示 | 完成前端接入 |
| Phase 7 | 生日礼（可选） | 依赖 auth 补生日字段 |

---

## 11. 后续二期方向

以下内容不在本期实现，但已在设计上预留扩展空间：

- 积分消费（商城兑换、抽奖）
- 下单积分抵扣
- 退款回滚积分/成长值
- 年度等级重算与降级策略
- 会员权益接入订单折扣、运费计算
- 更丰富的积分规则引擎

建议在本期上线稳定后，再为“积分消费”和“订单结算接入”分别单独出设计稿。
