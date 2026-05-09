# 微服务电商平台架构设计文档

> 创建日期：2026-05-08
> 状态：已确认

---

## 1. 项目概述

构建一个微服务架构的电商平台，覆盖管理后台、PC 用户端、微信小程序、Android App 四个端。

---

## 2. 技术选型

### 2.1 后端

| 技术 | 版本 | 说明 |
|---|---|---|
| Java + Spring Boot | 4.0 | 微服务基础框架 |
| Spring Cloud | 配套版本 | 微服务生态（Gateway + OpenFeign + LoadBalancer） |
| MyBatis-Plus | 3.5+ | ORM + 分页 |
| Nacos | 2.x (Docker) | 服务注册与发现 + 配置中心 |
| RocketMQ | 5.x (Docker) | 消息队列，异步解耦 |
| MySQL | 8.0 (Docker) | 数据库，每个服务独立库 |
| Redis | 7.2 (Docker) | 缓存 + Token 存储 + 秒杀 |
| MinIO | latest (Docker) | 对象存储（文件/图片） |
| Elasticsearch | latest (Docker, P2) | 商品搜索 |

### 2.2 前端

| 端 | 技术栈 | 阶段 |
|---|---|---|
| 管理后台 | Vue 3 + Vite + Element Plus | P0 |
| PC 用户端 | Vue 3 + Nuxt 3 (SSR) | P1 |
| 微信小程序 | uni-app (Vue 3) | P2 |
| Android App | Android Studio + Kotlin | P2 |

### 2.3 基础设施

所有中间件通过 `docker-compose.yml` 统一编排，一键启动：

| 组件 | 端口 | 凭证 |
|---|---|---|
| MySQL 8.0 | 3306 | root / root |
| Redis 7.2 | 6379 | (密码: root) |
| Nacos 2.x | 8848 | — |
| RocketMQ 5.x | 9876 (NameServer) | — |
| MinIO | 9000 (API) / 9001 (Console) | minio / minio |
| ES (P2) | 9200 | — |

---

## 3. 微服务清单

共 13 个后端服务，按阶段交付：

### P0 — 核心地基（6 个运行服务 + 1 个公共库 + 管理后台）

| 错误码前缀 | 服务 | 端口 | 职责 |
|---|---|---|---|
| — | ecommerce-common | — | 公共模块：Result、ErrorCode 接口、BusinessException、工具类、切面 |
| — | ecommerce-gateway | 8080 | Spring Cloud Gateway + JWT 鉴权拦截 |
| `01xx` | ecommerce-auth | 8091 | 用户认证（注册/登录/Token）+ 管理员认证 + RBAC 权限 |
| `02xx` | ecommerce-file | 8090 | 文件上传/删除/图片处理/对接 MinIO |
| `10xx` | ecommerce-user | 8081 | 用户信息/收货地址/个人中心 |
| `20xx` | ecommerce-product | 8082 | 商品分类/SPU/SKU/品牌/评论（含评分） |
| `30xx` | ecommerce-inventory | 8083 | 库存查询/扣减/预留/释放 |

### P1 — 交易闭环（3 个服务 + PC 端）

| 错误码前缀 | 服务 | 端口 | 职责 |
|---|---|---|---|
| `35xx` | ecommerce-cart | 8086 | 购物车 CRUD、Redis 缓存、未登录合并、选中结算 |
| `40xx` | ecommerce-order | 8084 | 下单/订单状态流转/订单查询 |
| `50xx` | ecommerce-payment | 8085 | 微信支付/支付回调/退款/对账 |

### P2 — 扩展能力（4 个服务 + 小程序 + App）

| 错误码前缀 | 服务 | 端口 | 职责 |
|---|---|---|---|
| `60xx` | ecommerce-coupon | 8087 | 优惠券（满减/折扣）/促销活动 |
| `70xx` | ecommerce-notification | 8088 | 短信/邮件/小程序订阅消息/App 推送 |
| `80xx` | ecommerce-search | 8089 | ES 商品搜索（分词/筛选/排序） |
| `90xx` | ecommerce-seckill | 8092 | 秒杀专场/Redis 预扣库存/MQ 削峰 |

---

## 4. 项目结构（模块化单体仓库）

```
ecommerce-platform/
├── ecommerce-common/          公共模块
├── ecommerce-gateway/         网关
├── ecommerce-auth/            认证服务
├── ecommerce-file/            文件服务
├── ecommerce-user/            用户服务
├── ecommerce-product/         商品服务
├── ecommerce-inventory/       库存服务
├── ecommerce-cart/            购物车服务
├── ecommerce-order/           订单服务
├── ecommerce-payment/         支付服务
├── ecommerce-coupon/          营销服务
├── ecommerce-notification/    通知服务
├── ecommerce-search/          搜索服务
├── ecommerce-seckill/         秒杀服务
├── ecommerce-admin/           管理后台 (Vue 3)
├── ecommerce-pc/              PC 用户端 (Vue 3 + Nuxt)
├── ecommerce-mp/              微信小程序 (uni-app)
├── docker-compose.yml         中间件 Docker 编排
├── docs/                      文档
└── pom.xml                    父 POM
```

---

## 5. 微服务内部分层规范

每个微服务统一采用以下分层结构：

```
com.ecommerce.{service}/
├── controller/          ← REST 接口层（薄层，只做参数绑定和调用 Service）
├── service/
│   └── impl/            ← 业务逻辑层（所有业务逻辑在此）
├── mapper/              ← MyBatis-Plus Mapper
├── entity/              ← 数据库实体（PO）
├── dto/
│   ├── request/         ← 入参 DTO
│   └── response/        ← 出参 DTO
└── config/              ← 本服务专属配置
```

### 核心约束

- **Controller 不含业务逻辑**：只做参数校验、调用 Service、封装返回
- **Service 层包含所有业务逻辑**：事务边界、业务判断、数据组装
- **统一返回 `Result<T>`**：所有接口返回 `Result<T>`（code + message + data）
- **枚举错误码**：错误码用枚举定义，不硬编码
- **英文命名**：类名、方法名、变量名全部英文，禁止拼音

---

## 6. 公共模块设计（ecommerce-common）

```
com.ecommerce.common/
├── result/
│   ├── Result.java              ← 统一返回体 Result<T>
│   ├── ErrorCode.java           ← 错误码接口（各服务枚举实现此接口）
│   └── BusinessException.java   ← 通用业务异常
├── exception/
│   └── GlobalExceptionHandler.java  ← 全局异常处理
├── config/
│   ├── JacksonConfig.java       ← JSON 序列化配置
│   ├── CorsConfig.java          ← 跨域配置
│   └── MybatisPlusConfig.java   ← 分页拦截器
├── util/
│   ├── JwtUtils.java            ← JWT 工具类
│   ├── SnowflakeIdUtils.java    ← 雪花 ID 生成
│   └── RedisUtils.java          ← Redis 工具类
└── constant/
    └── CommonConstants.java     ← 公共常量
```

---

## 7. 错误码规范

### 设计原则

- Common 只定义 `ErrorCode` 接口（契约），不定义具体错误码
- 各服务独立定义自己的错误码枚举，实现 `ErrorCode` 接口
- 错误码为 8 位整数：`X XX XXXXX`

```
X       → 服务标识位（1 位）
  XX    → 一级分类（2 位）
    XXXXX → 具体错误（5 位）
```

### 错误码分配表

| 前缀 | 服务 | 编号范围 | 示例 |
|---|---|---|---|
| `0` | Common 通用 | 00001001 - 00001999 | 00001001=系统繁忙 |
| `01` | ecommerce-auth | 01001001 - 01001999 | 01001001=Token 无效 |
| `02` | ecommerce-file | 02001001 - 02001999 | 02001001=文件大小超限 |
| `10` | ecommerce-user | 10001001 - 10001999 | 10001001=用户不存在 |
| `20` | ecommerce-product | 20001001 - 20001999 | 20001001=商品不存在 |
| `30` | ecommerce-inventory | 30001001 - 30001999 | 30001001=库存不足 |
| `35` | ecommerce-cart | 35001001 - 35001999 | 35001001=购物车不存在 |
| `40` | ecommerce-order | 40001001 - 40001999 | 40001001=订单不存在 |
| `50` | ecommerce-payment | 50001001 - 50001999 | 50001001=支付失败 |
| `60` | ecommerce-coupon | 60001001 - 60001999 | 60001001=优惠券已过期 |
| `70` | ecommerce-notification | 70001001 - 70001999 | 70001001=发送失败 |
| `80` | ecommerce-search | 80001001 - 80001999 | 80001001=索引异常 |
| `90` | ecommerce-seckill | 90001001 - 90001999 | 90001001=秒杀活动不存在 |

### 代码示例

```java
// common - 接口契约
public interface ErrorCode {
    int getCode();
    String getMessage();
}

// common - 业务异常
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    // getter...
}

// user 服务 - 自己的枚举
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(10001001, "用户不存在"),
    USERNAME_DUPLICATE(10001002, "用户名已存在"),
    PASSWORD_ERROR(10001003, "密码错误"),
    TOKEN_EXPIRED(10001004, "Token 已过期"),
    ;

    private final int code;
    private final String message;
    // constructor + getter...
}
```

---

## 8. API 设计规范

### RESTful 风格

```
GET    /api/v1/products             分页查询商品
GET    /api/v1/products/{id}        商品详情
POST   /api/v1/products             新增商品
PUT    /api/v1/products/{id}        更新商品
DELETE /api/v1/products/{id}        删除商品

GET    /api/v1/orders               订单列表
GET    /api/v1/orders/{orderNo}     订单详情
POST   /api/v1/orders               创建订单
```

### 统一返回体

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 业务失败
{ "code": 30001001, "message": "库存不足", "data": null }

// 分页
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [...],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 参数校验

使用 Jakarta Validation（`@Valid` + `@NotBlank` / `@NotNull` 等），校验失败由 `GlobalExceptionHandler` 统一处理返回。

---

## 9. 数据库设计原则

### 服务独立数据库

```
ecommerce_user        ← 用户服务独占
ecommerce_product     ← 商品服务独占（含评论表）
ecommerce_inventory   ← 库存服务独占
ecommerce_order       ← 订单服务独占
ecommerce_payment     ← 支付服务独占
ecommerce_coupon      ← 营销服务独占
ecommerce_cart        ← 购物车服务独占
ecommerce_auth        ← 认证服务独占
```

### 核心原则

- 每个微服务拥有自己的数据库，不允许跨服务直接访问数据库
- 服务间数据交互只能通过 API（OpenFeign）或消息队列（RocketMQ）
- 每表必有 `id`（雪花算法）、`created_at`、`updated_at`、`deleted`（逻辑删除）

---

## 10. 购物车服务设计（ecommerce-cart，P1）

### 10.1 为什么独立

| 维度 | 说明 |
|---|---|
| **数据归属** | 购物车归属于"会话"，不属于订单。用户可能反复修改购物车再下单 |
| **缓存依赖** | 购物车重度依赖 Redis，未登录用户购物车纯 Redis 存储，读写模式跟订单完全不同 |
| **扩展方向** | 后续加"降价提醒""库存紧张""凑单推荐"等，都是购物车维度的功能 |

### 10.2 数据库设计

```
ecommerce_cart/
├── cart            ← 购物车（一个用户一条记录）
└── cart_item       ← 购物车明细（每条一个 SKU）
```

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_cart CHARACTER SET utf8mb4;

USE ecommerce_cart;

CREATE TABLE cart (
    id          BIGINT  NOT NULL PRIMARY KEY,
    user_id     BIGINT  NOT NULL COMMENT '用户ID',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_item (
    id          BIGINT  NOT NULL PRIMARY KEY,
    cart_id     BIGINT  NOT NULL,
    sku_id      BIGINT  NOT NULL COMMENT 'SKU ID',
    spu_id      BIGINT  NOT NULL COMMENT 'SPU ID（冗余，方便展示商品信息）',
    quantity    INT     NOT NULL DEFAULT 1 COMMENT '数量',
    selected    TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选结算',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cart_id (cart_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 10.3 Redis 策略

```
Key 设计：
  cart:guest:{guestId}    → Hash  {skuId → CartItemVO JSON}   未登录购物车
  cart:user:{userId}      → Hash  {skuId → CartItemVO JSON}   已登录购物车
  TTL: 30 天
```

**登录合并流程：**
```
用户登录 → 查 cart:guest:{guestId} → 有数据？
  → 有：遍历 guest cart items → 逐个合并到 cart:user:{userId}
         （相同 SKU 数量叠加，不同 SKU 追加）
         → DEL cart:guest:{guestId}
         → 同步写入 MySQL cart + cart_item
  → 无：从 MySQL 加载 cart:user:{userId} 到 Redis
```

**读优先：** 所有读操作读 Redis（热数据），MySQL 做持久化兜底。
**写策略：** Cache-Aside（先写 MySQL，再更新 Redis）。

### 10.4 API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/v1/cart` | 获取购物车（含所有 cartItem，商品信息从 Product 服务查） |
| `POST` | `/api/v1/cart/items` | 添加商品到购物车 `{skuId, quantity}` |
| `PUT` | `/api/v1/cart/items/{itemId}` | 修改数量 `{quantity}` |
| `DELETE` | `/api/v1/cart/items/{itemId}` | 删除购物车项 |
| `PUT` | `/api/v1/cart/items/{itemId}/select` | 勾选/取消勾选 `{selected}` |
| `PUT` | `/api/v1/cart/select-all` | 全选/取消全选 `{selected}` |
| `DELETE` | `/api/v1/cart` | 清空购物车 |

### 10.5 结算时的服务交互

```
PC 端点击"去结算"
  → Cart Service: 获取 selected=true 的 cartItems
  → Product Service: 查 SKU 最新价格（防价格变动）
  → Inventory Service: 校验库存是否充足
  → 返回结算确认页数据（含实时价格 + 库存状态）
  
用户确认下单
  → Order Service: 创建订单（从 Cart 传入 selected items）
  → Cart Service: 删除已下单的 cartItem
```

### 10.6 错误码

| 错误码 | 说明 |
|---|---|
| `35001001` | 购物车不存在 |
| `35001002` | SKU 已下架 |
| `35001003` | 库存不足 |
| `35001004` | 购物车项不存在 |
| `35001005` | 数量超出限制 |

---

## 11. 服务间通信

### 同步通信：OpenFeign

用于实时查询/校验场景：

- 下单时查用户地址（Order → User）
- 下单时查商品信息（Order → Product）
- 下单时校验库存（Order → Inventory）

### 异步通信：RocketMQ

用于解耦非实时操作：

| Topic | 生产者 | 消费者 | 说明 |
|---|---|---|---|
| `order-created` | Order | Inventory + Notification | 下单后扣库存、发通知 |
| `order-paid` | Payment | Order + Inventory | 支付成功后更新订单、扣真实库存 |
| `order-cancelled` | Order | Inventory | 取消订单后释放库存 |
| `product-status-changed` | Product | Search | 商品上下架同步 ES |
| `stock-changed` | Inventory | Search | 库存变更同步 ES 搜索数据 |

---

## 12. Gateway 认证架构

```
用户端请求 → Gateway
               ├→ 白名单（/api/v1/auth/login、/api/v1/auth/register）→ 直接放行
               ├→ 需要登录 → 调用 Auth Service 校验 JWT → 放行/拦截
               └→ 管理员接口 → 调用 Auth Service 校验 JWT + RBAC 权限 → 放行/拦截
```

### Auth Service 职责

- C 端用户：手机号注册/登录、微信登录、JWT 签发与刷新
- 管理端：用户名+密码登录、JWT 签发、RBAC（用户→角色→权限→菜单）

---

## 13. 分阶段交付计划

### P0 — 核心地基

**目标：** 基础设施就绪，管理后台能管理商品和用户

**后端模块（7个 — 6 个运行服务 + 1 个公共库）：**
- ecommerce-common（公共模块）
- ecommerce-gateway（网关 + 鉴权）
- ecommerce-auth（认证 + RBAC）
- ecommerce-file（文件服务 + MinIO）
- ecommerce-user（用户服务）
- ecommerce-product（商品服务，含评论）
- ecommerce-inventory（库存服务）

**前端：**
- 管理后台：登录、商品管理、分类管理、库存管理、用户列表、文件上传

### P1 — 交易闭环

**目标：** PC 端用户能完成完整购买流程

**新增后端服务（2个）：**
- ecommerce-order（购物车 + 订单）
- ecommerce-payment（微信支付）

**前端：**
- PC 用户端：首页、商品搜索/列表/详情、购物车、下单、支付、订单查询、个人中心
- 管理后台扩展：订单管理（列表/详情/发货）、数据概览

### P2 — 多端覆盖 + 扩展能力

**新增后端服务（4个）：**
- ecommerce-coupon（营销）
- ecommerce-notification（通知）
- ecommerce-search（ES 搜索）
- ecommerce-seckill（秒杀）

**前端：**
- 微信小程序（uni-app）
- Android App（Kotlin 原生）

---

## 14. 架构图

```
                     ┌──────────────────────────────────────┐
                     │            Nginx / CDN                │
                     └──────────────┬───────────────────────┘
                                    │
                     ┌──────────────▼───────────────────────┐
                     │   Spring Cloud Gateway :8080          │
                     │   (路由分发 + JWT 鉴权)                 │
                     └──┬───┬───┬───┬───┬───┬───┬──────────┘
                        │   │   │   │   │   │   │
         ┌──────────────┤   │   │   │   │   │   │
         │              │   │   │   │   │   │   │
         ▼              ▼   ▼   ▼   ▼   ▼   ▼   ▼
    ┌────────┐    ┌────┬───┬───┬───┬───┬───┬───┬────┐
    │ Nacos  │    │Auth│Fl │Usr│Prd│Inv│Ord│Pay│ ... │
    │ :8848  │    │:91 │:90│:81│:82│:83│:84│:85│     │
    └────────┘    └──┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─────┘
                     │   │   │   │   │   │   │
                     └───┼───┼───┼───┼───┼───┘
                         │   │   │   │   │
              ┌──────────┼───┼───┼───┼───┼──────────┐
              │          │   │   │   │   │            │
              ▼          ▼   ▼   ▼   ▼   ▼            ▼
        ┌─────────┐ ┌──────┐ ┌───────┐ ┌──────┐ ┌──────┐
        │RocketMQ │ │MySQL │ │ Redis │ │MinIO │ │  ES  │
        │:9876    │ │:3306 │ │ :6379 │ │:9000 │ │:9200 │
        └─────────┘ └──────┘ └───────┘ └──────┘ └──────┘
                                                  (P2)
```

---

## 附录：备忘信息

| 项目 | 信息 |
|---|---|
| MySQL root 密码 | root |
| Redis 密码 | root |
| MinIO 凭证 | minio / minio |
| JWT 签名密钥 | 待配置（建议 RSA256） |
| 端口范围 | 8080-8091（后端服务） |
