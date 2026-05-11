# 微服务电商平台架构设计文档

> 创建日期：2026-05-08
> 最后更新：2026-05-11
> 状态：已确认

---

## 1. 项目概述

构建一个微服务架构的电商平台，覆盖管理后台、PC 用户端、微信小程序、Android App 四个端。

---

## 2. 技术选型

### 2.1 后端

| 技术 | 版本 | 说明 |
|---|---|---|
| Java | 21 | LTS 长期支持版本 |
| Spring Boot | 4.0.0 | 微服务基础框架 |
| Spring Cloud | 2025.1.1 | 微服务生态（Gateway + OpenFeign + LoadBalancer） |
| MyBatis-Plus | 3.5.16 | ORM + 分页 + 逻辑删除 |
| Nacos | 2.4.0 (Docker) | 服务注册与发现 + 配置中心 |
| RocketMQ | 5.2.0 (Docker) | 消息队列，异步解耦，spring-boot-starter 2.3.0 |
| MySQL | 8.0 (Docker) | 数据库，每个服务独立库 |
| Redis | 7.2 (Docker) | 缓存 + Token 存储 + 购物车 |
| MinIO | latest (Docker) | 对象存储（文件/图片） |
| Elasticsearch | latest (Docker, P2) | 商品搜索 |

### 2.2 前端

| 端 | 技术栈 | 阶段 |
|---|---|---|
| 管理后台 | Vue 3 + Vite + Element Plus | P0 |
| PC 用户端 | Vue 3 + Nuxt 3 (SSR) | P1 |
| 微信小程序 | uni-app (Vue 3) | P2 |
| Android App | Android Studio + Kotlin | P2 |

### 2.3 工具库

| 库 | 版本 | 说明 |
|---|---|---|
| Lombok | 1.18.46 | 简化 Java 代码（@Data, @NoArgsConstructor 等） |
| Hutool | 5.8.44 | 工具类库（雪花 ID 等） |
| jjwt | 0.12.6 | JWT 令牌签发与校验 |
| MinIO Client | 8.6.0 | 对象存储 SDK |

### 2.4 基础设施

所有中间件通过 `docker-compose.yml` 统一编排，一键启动：

| 组件 | 端口 | 凭证 / 备注 |
|---|---|---|
| MySQL 8.0 | 3306 | root / root |
| Redis 7.2 | 6379 | 密码: root |
| Nacos 2.4.0 | 8848 (gRPC: 9848) | standalone 模式 |
| RocketMQ 5.2.0 | 9876 (NameServer), 10911 (Broker) | autoCreateTopicEnable=true |
| MinIO | 9000 (API) / 9001 (Console) | minio / minioadmin |
| ES (P2) | 9200 | — |

---

## 3. 微服务清单

### 当前已实现（P0–P1，11 个运行服务 + 1 个公共库）

| 错误码前缀 | 服务 | 端口 | 数据库 | 职责 |
|---|---|---|---|---|
| — | ecommerce-common | — | — | 公共模块：Result、ErrorCode、BusinessException、BaseEntity、JWT/Snowflake 工具、全局异常处理 |
| — | ecommerce-gateway | 8080 | — | Spring Cloud Gateway + JWT 鉴权 + 多角色权限拦截 |
| `01xx` | ecommerce-auth | 8091 | ecommerce_auth | 用户注册/登录/Token 签发、管理员认证、RBAC 权限管理 |
| `02xx` | ecommerce-file | 8090 | — | 文件上传/下载、对接 MinIO 对象存储 |
| `10xx` | ecommerce-user | 8081 | ecommerce_user | 用户信息管理、收货地址 CRUD |
| `20xx` | ecommerce-product | 8082 | ecommerce_product | 商品分类、品牌、SPU/SKU 管理、商品评论 |
| `30xx` | ecommerce-inventory | 8083 | ecommerce_inventory | 库存扣减/释放/初始化、库存查询 |
| `35xx` | ecommerce-cart | 8086 | ecommerce_cart | 购物车 CRUD、Redis 缓存、登录合并 |
| `40xx` | ecommerce-order | 8084 | ecommerce_order | 下单、订单状态流转、订单查询（用户/商家/管理员视角） |
| `50xx` | ecommerce-payment | 8085 | ecommerce_payment | 支付、退款、对账、结算 |
| `60xx` | ecommerce-merchant | 8087 | ecommerce_merchant | 商家入驻/审核/管理、商家管理员账号自动创建 |

### P2 — 扩展能力（4 个服务 + 小程序 + App）

| 错误码前缀 | 服务 | 端口 | 职责 |
|---|---|---|---|
| `70xx` | ecommerce-coupon | TBD | 优惠券（满减/折扣）/促销活动 |
| `80xx` | ecommerce-notification | TBD | 短信/邮件/小程序订阅消息/App 推送 |
| `90xx` | ecommerce-search | TBD | ES 商品搜索（分词/筛选/排序） |
| `95xx` | ecommerce-seckill | TBD | 秒杀专场/Redis 预扣库存/MQ 削峰 |

> **变更说明**：原 P2 的 ecommerce-coupon 端口 8087 已分配给 ecommerce-merchant，错误码前缀 60 也已占用。P2 服务的端口和错误码前缀需在上线前重新分配。

---

## 4. 项目结构（Maven 多模块单体仓库）

```
ecommerce-platform/
├── ecommerce-common/          公共模块
├── ecommerce-gateway/         网关
├── ecommerce-auth/            认证服务
├── ecommerce-file/            文件服务
├── ecommerce-user/            用户服务
├── ecommerce-product/         商品服务
├── ecommerce-inventory/       库存服务
├── ecommerce-merchant/        商家服务  ★ 新增
├── ecommerce-cart/            购物车服务
├── ecommerce-order/           订单服务
├── ecommerce-payment/         支付服务
├── ecommerce-admin/           管理后台 (Vue 3)
├── ecommerce-web/             PC 用户端 (Vue 3)
├── docker-compose.yml         中间件 Docker 编排
├── docker/rocketmq/           RocketMQ 配置与初始化脚本
│   ├── broker.conf            Broker 配置（autoCreateTopicEnable=true）
│   └── init-topics.sh         Topic 预创建脚本
├── docs/                      文档与规格说明
└── pom.xml                    父 POM（统一依赖版本管理）
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
├── entity/              ← 数据库实体（PO，继承 BaseEntity）
├── dto/
│   ├── request/         ← 入参 DTO
│   └── response/        ← 出参 DTO / VO
├── consumer/            ← RocketMQ 消费者（如有）
├── client/              ← OpenFeign 客户端（如有）
├── common/              ← 本服务专属（ErrorCode 枚举等）
└── config/              ← 本服务专属配置
```

### 核心约束

- **Controller 不含业务逻辑**：只做参数校验、调用 Service、封装返回
- **Service 层包含所有业务逻辑**：事务边界、业务判断、数据组装
- **统一返回 `Result<T>`**：所有接口返回 `Result<T>`（code + message + data）
- **枚举错误码**：错误码用枚举定义实现 ErrorCode 接口，不硬编码
- **英文命名**：类名、方法名、变量名、表名、字段名全部英文，禁止拼音
- **DTO/VO/Entity 统一使用 Lombok**：`@Data` + 按需 `@NoArgsConstructor`/`@AllArgsConstructor`
- **Long 类型序列化为 String**：通过 JacksonConfig 全局配置 `JsonMapper.builder().configure(ToJsonString)`，解决 JavaScript 长整型精度丢失问题

---

## 6. 公共模块设计（ecommerce-common）

```
com.ecommerce.common/
├── result/
│   ├── Result.java                ← 统一返回体 Result<T>（code + message + data）
│   ├── ErrorCode.java             ← 错误码接口（各服务枚举实现此接口）
│   └── BusinessException.java     ← 通用业务异常
├── exception/
│   └── GlobalExceptionHandler.java ← 全局异常处理（BusinessException / Validation / Exception）
├── config/
│   ├── JacksonConfig.java         ← Jackson 全局配置（Long→String, JavaTimeModule）
│   └── MetaObjectHandlerConfig.java ← MyBatis-Plus 自动填充（createdAt/updatedAt/deleted）
├── entity/
│   └── BaseEntity.java            ← 实体基类（id + createdAt + updatedAt + deleted 逻辑删除）
├── dto/
│   ├── OrderPaidMessage.java      ← MQ 消息体：支付成功通知
│   ├── OrderInventoryMessage.java ← MQ 消息体：库存操作（含 OrderItemMessage 列表）
│   ├── OrderItemMessage.java      ← MQ 消息体：订单项（skuId + quantity）
│   ├── ProductCreatedMessage.java ← MQ 消息体：商品创建通知
│   ├── MerchantApprovedMessage.java ← MQ 消息体：商家审核通过通知
│   └── ...                        ← 其他公共 DTO/VO
├── util/
│   ├── JwtUtils.java              ← JWT 工具类（签发/解析/验证）
│   └── SnowflakeUtils.java        ← Hutool Snowflake ID 生成
└── annotation/
    └── ...                        ← 公共注解
```

---

## 7. 错误码规范

### 设计原则

- Common 只定义 `ErrorCode` 接口（契约），不定义具体错误码
- 各服务独立定义自己的错误码枚举，实现 `ErrorCode` 接口
- 错误码为 8 位整数：`XX XXX XXX`

```
XX      → 服务前缀（2 位）
  XXX   → 一级分类（3 位）
     XXX → 具体错误（3 位）
```

### 错误码分配表

| 前缀 | 服务 | 编号范围 | 示例 |
|---|---|---|---|
| `00` | Common 通用 | 00001001 - 00001999 | 00001001=系统繁忙 |
| `01` | ecommerce-auth | 01001001 - 01001999 | 01001001=Token 无效 |
| `02` | ecommerce-file | 02001001 - 02001999 | 02001001=文件大小超限 |
| `10` | ecommerce-user | 10001001 - 10001999 | 10001001=用户不存在 |
| `20` | ecommerce-product | 20001001 - 20001999 | 20001001=商品不存在 |
| `30` | ecommerce-inventory | 30001001 - 30001999 | 30001001=库存不足 |
| `35` | ecommerce-cart | 35001001 - 35001999 | 35001001=购物车不存在 |
| `40` | ecommerce-order | 40001001 - 40001999 | 40001001=订单不存在 |
| `50` | ecommerce-payment | 50001001 - 50001999 | 50001001=支付失败 |
| `60` | ecommerce-merchant | 60001001 - 60001999 | 60001001=商家不存在 |
| `70` | ecommerce-coupon (P2) | 70001001 - 70001999 | 70001001=优惠券已过期 |
| `80` | ecommerce-notification (P2) | 80001001 - 80001999 | 80001001=发送失败 |
| `90` | ecommerce-search (P2) | 90001001 - 90001999 | 90001001=索引异常 |

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
    public ErrorCode getErrorCode() { return errorCode; }
}

// merchant 服务 - 枚举实现
public enum MerchantErrorCode implements ErrorCode {
    MERCHANT_NOT_FOUND(60010001, "商家不存在"),
    MERCHANT_NAME_EXISTS(60010002, "店铺名称已存在"),
    MERCHANT_NOT_PENDING(60010004, "商家不在待审核状态"),
    ;

    private final int code;
    private final String message;
    MerchantErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
```

---

## 8. API 设计规范

### RESTful 风格

```
GET    /api/v1/products             分页查询商品
GET    /api/v1/products/{id}        商品详情
POST   /api/v1/admin/products       新增商品（管理端）
PUT    /api/v1/admin/products/{id}  更新商品
DELETE /api/v1/admin/products/{id}  删除商品

POST   /api/v1/orders               创建订单
GET    /api/v1/orders/{id}          订单详情
GET    /api/v1/admin/orders         订单列表（管理端）

POST   /api/v1/merchants/register   商家入驻
PUT    /api/v1/admin/merchants/{id}/audit  商家审核
GET    /api/v1/admin/merchants      商家列表
```

### 路径约定

- `/api/v1/` 前缀：用户端接口（C 端）
- `/api/v1/admin/` 前缀：管理端接口（需 admin 角色）
- `/api/v1/internal/` 前缀：服务间内部调用（OpenFeign）

### 管理端鉴权模型

Gateway AuthFilter 支持 **4 种角色**：

| 角色 | JWT type | 权限范围 |
|---|---|---|
| `super_admin` | super_admin | 所有管理接口 |
| `admin` | admin | 商品/订单管理 |
| `ops` | ops | 可看 dashboard，不能审核商家/管理用户 |
| `merchant` | merchant | 仅管理自己的商品和订单，不能访问商家审核/用户管理 |

### 统一返回体

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 业务失败（枚举错误码）
{ "code": 30001001, "message": "库存不足", "data": null }

// 参数校验失败
{ "code": 400, "message": "name: 商品名称不能为空; price: 价格格式不正确", "data": null }
```

### 参数校验

使用 Jakarta Validation（`@Valid` + `@NotBlank` / `@NotNull` 等），校验失败由 `GlobalExceptionHandler.handleValidation()` 统一处理返回。

---

## 9. 数据库设计原则

### 服务独立数据库（当前已建库）

```
ecommerce_auth          ← 认证服务（admin_user, role, permission, role_permission, admin_user_role）
ecommerce_product       ← 商品服务（spu, sku, category, brand, review）
ecommerce_inventory     ← 库存服务（stock）
ecommerce_order         ← 订单服务（order, order_item）
ecommerce_payment       ← 支付服务（payment, refund）
ecommerce_cart          ← 购物车服务（cart, cart_item）
ecommerce_merchant      ← 商家服务（merchant, merchant_audit）
ecommerce_user          ← 用户服务（address）
```

### 核心原则

- 每个微服务拥有自己的数据库，**禁止跨服务直接访问数据库**
- 服务间数据交互只能通过 **API（OpenFeign）** 或 **消息队列（RocketMQ）**
- 所有表继承 `BaseEntity` 字段：`id`（雪花算法）、`created_at`、`updated_at`、`deleted`（逻辑删除）
- 表名、字段名使用下划线命名（snake_case），Java 实体使用驼峰命名（camelCase）

---

## 10. 消息队列设计（RocketMQ）

### 10.1 基础设施

| 配置项 | 值 |
|---|---|
| RocketMQ 版本 | 5.2.0 (Docker) |
| Spring Boot Starter | rocketmq-spring-boot-starter 2.3.0 |
| NameServer | localhost:9876 |
| Broker | 192.168.5.6:10911 |
| 序列化方式 | JSON |
| autoCreateTopicEnable | true（broker.conf） |

### 10.2 Topic 清单

| Topic | Producer | Consumer（Group） | 说明 |
|---|---|---|---|
| `order-created` | ecommerce-order | ecommerce-inventory (`inventory-deduct`) | 下单后异步扣减库存 |
| `order-cancelled` | ecommerce-order | ecommerce-inventory (`inventory-release`) | 取消订单后释放库存 |
| `order-paid` | ecommerce-payment | ecommerce-order (`order-consumer`) | 支付成功后更新订单状态 |
| `product-created` | ecommerce-product | ecommerce-inventory (`inventory-init`) | 创建商品时初始化库存为 0 |
| `merchant-approved` | ecommerce-merchant | ecommerce-auth (`auth-consumer`) | 商家审核通过后创建管理员账号 |

### 10.3 消息体定义（均位于 ecommerce-common）

| 消息类 | 字段 | 序列化要求 |
|---|---|---|
| `OrderInventoryMessage` | `String orderNo`, `List<OrderItemMessage> items` | @Data @NoArgsConstructor @AllArgsConstructor |
| `OrderItemMessage` | `Long skuId`, `Integer quantity` | 同上 |
| `OrderPaidMessage` | `String orderNo`, `Integer status`, `LocalDateTime paidAt` | 同上 |
| `ProductCreatedMessage` | `Long spuId`, `Long skuId` | 同上 |
| `MerchantApprovedMessage` | `Long merchantId`, `String merchantName` | 同上 |

> **关键约束**：所有 MQ 消息 DTO 必须同时标注 `@Data` + `@NoArgsConstructor` + `@AllArgsConstructor`。
> `@NoArgsConstructor` 是 Spring Messaging Jackson 2.x 反序列化的必要构造器，缺失会导致 `Cannot construct instance` 异常。

### 10.4 消费者设计原则

- 每个消费者使用 **独立的 consumer group**（不能多个消费者共用同一 group）
- 共 group 会导致后启动的消费者订阅被覆盖，仅最后一个 consumer 的 topic 生效
- Consumer group 命名：`${spring.application.name}-{功能后缀}`

```
错误示例（会导致订阅冲突）：
  InventoryDeductListener      → group: ecommerce-inventory-v2
  InventoryReleaseListener     → group: ecommerce-inventory-v2  ← 冲突！

正确示例：
  InventoryDeductListener      → group: ecommerce-inventory-deduct
  InventoryReleaseListener     → group: ecommerce-inventory-release
  InventoryInitStockListener   → group: ecommerce-inventory-init
```

### 10.5 Topic 初始化

RocketMQ 的 `autoCreateTopicEnable` 仅在 Producer 首次发送消息时自动创建 Topic。消费者在服务启动时订阅，若 Topic 此时不存在则订阅失败且不会自动重连。

**解决方案**：在所有微服务启动前执行初始化脚本：

```bash
bash docker/rocketmq/init-topics.sh
```

脚本会预创建全部 5 个 Topic（每个 4 读 4 写队列），确保各服务消费者启动时 Topic 已就绪。

---

## 11. 服务间通信

### 同步通信：OpenFeign

用于实时查询/校验场景：

| 调用方 | 被调用方 | 场景 |
|---|---|---|
| ecommerce-order | ecommerce-cart | 获取购物车数据用于下单 |
| ecommerce-order | ecommerce-product | 查询商品 SPU/SKU 信息、按商家查商品 ID |
| ecommerce-payment | ecommerce-order | 通过 orderNo 查订单详情 |
| ecommerce-merchant | ecommerce-auth | 创建商家管理员（已改为 MQ） |
| ecommerce-product | ecommerce-inventory | 库存校验（如有） |

### 异步通信：RocketMQ

用于解耦非实时操作（详见第 10 节）。

---

## 12. Gateway 认证架构

```
用户端请求 → Gateway (:8080)
               ├→ 白名单 POST 放行：
               │    /api/v1/auth/login
               │    /api/v1/auth/register
               │    /api/v1/auth/admin/login
               │    /api/v1/merchants/register
               ├→ 公开 GET 放行：
               │    /api/v1/products
               │    /api/v1/categories
               │    /api/v1/reviews
               │    /api/v1/files
               ├→ admin 路径 → 校验 JWT + admin/super_admin/ops/merchant 角色
               │    └→ 角色权限矩阵过滤（merchant 不能管用户/商家）
               └→ 其他路径 → 校验 JWT（登录即可）
```

### 认证流程

1. Gateway AuthFilter 解析 `Authorization: Bearer <token>` 
2. 调用 `JwtUtils.parse(token)` 校验签名和有效期
3. 从 JWT Claims 提取 `sub`(userId)、`type`(userType)、`role`、`merchantId`
4. 注入下游请求头：`X-User-Id`、`X-User-Type`、`X-Merchant-Id`
5. admin 路径额外校验 role 字段，非 admin 角色返回 403

---

## 13. Jackson 序列化配置

```java
// JacksonConfig — 全局配置
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> builder
            .postConfigurer(mapper -> 
                ((JsonMapper) mapper).configure(
                    com.fasterxml.jackson.databind.cfg.ToJsonStringFeature
                        .WRITE_LONG_AS_STRING, true
                )
            )
            .modules(new JavaTimeModule());
    }
}
```

**作用**：所有 Long 类型序列化为 JSON 时输出为 String，解决 JavaScript Number 精度最多 53 位而 Java Long 64 位的问题。前端接收到的 `id` 始终是字符串格式。

---

## 14. 架构图

```
                     ┌──────────────────────────────────────┐
                     │            Nginx / CDN                │
                     └──────────────┬───────────────────────┘
                                    │
                     ┌──────────────▼───────────────────────┐
                     │   Spring Cloud Gateway :8080          │
                     │   (路由分发 + JWT 鉴权 + 角色控制)      │
                     └──┬───┬───┬───┬───┬───┬───┬───┬──────┘
                        │   │   │   │   │   │   │   │
         ┌──────────────┤   │   │   │   │   │   │   │
         │              │   │   │   │   │   │   │   │
         ▼              ▼   ▼   ▼   ▼   ▼   ▼   ▼   ▼
    ┌────────┐    ┌────┬───┬───┬───┬───┬───┬───┬───┬────┐
    │ Nacos  │    │Auth│Fl │Usr│Prd│Inv│Crt│Ord│Pay│Mch │
    │ :8848  │    │:91│:90│:81│:82│:83│:86│:84│:85│:87 │
    └────────┘    └──┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴─┬─┴────┘
                     │   │   │   │   │   │   │   │
                     └───┼───┼───┼───┼───┼───┼───┘
                         │   │   │   │   │   │
              ┌──────────┼───┼───┼───┼───┼───┼──────────┐
              │          │   │   │   │   │   │            │
              ▼          ▼   ▼   ▼   ▼   ▼   ▼            ▼
        ┌─────────┐ ┌──────┐ ┌───────┐ ┌──────┐ ┌──────┐
        │RocketMQ │ │MySQL │ │ Redis │ │MinIO │ │  ES  │
        │:9876    │ │:3306 │ │ :6379 │ │:9000 │ │:9200 │
        └─────────┘ └──────┘ └───────┘ └──────┘ └──────┘
                                                  (P2)

服务端口速查:
  8080 gateway    8082 product    8084 order     8086 cart      8090 file
  8081 user       8083 inventory  8085 payment   8087 merchant  8091 auth
```

---

## 15. 商家服务设计（ecommerce-merchant）

### 15.1 概述

商家服务管理商家入驻、审核和基本信息。审核通过后通过 MQ 自动在 auth 服务创建商家管理员账号。

### 15.2 数据库设计

```
ecommerce_merchant/
├── merchant         ← 商家信息（名称、联系方式、营业执照、状态）
└── merchant_audit   ← 审核记录（审核人、审核动作、审核意见）
```

```sql
CREATE TABLE merchant (
    id               BIGINT       NOT NULL PRIMARY KEY,
    name             VARCHAR(128) NOT NULL COMMENT '店铺名称',
    logo             VARCHAR(512) COMMENT '店铺Logo',
    contact_name     VARCHAR(64)  COMMENT '联系人',
    contact_phone    VARCHAR(20)  COMMENT '联系电话',
    business_license VARCHAR(512) COMMENT '营业执照',
    status           TINYINT      DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回 3已关停',
    reason           VARCHAR(512) COMMENT '审核意见',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT      DEFAULT 0
);

CREATE TABLE merchant_audit (
    id               BIGINT       NOT NULL PRIMARY KEY,
    merchant_id      BIGINT       NOT NULL COMMENT '商家ID',
    auditor_id       BIGINT       COMMENT '审核人ID',
    action           TINYINT      NOT NULL COMMENT '1通过 2驳回 3关停',
    comment          VARCHAR(512) COMMENT '审核意见',
    created_at       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted          TINYINT      DEFAULT 0,
    INDEX idx_merchant_id (merchant_id)
);
```

### 15.3 审核流程

```
商家注册 → merchant.status = 0（待审核）
          → 管理员审核 → action=1（通过）→ 更新 status=1
                       │                 → 发送 MQ merchant-approved
                       │                 → Auth 消费 → 创建 admin_user(type=merchant)
                       │
                       → action=2（驳回）→ 更新 status=2，记录原因
                       → action=3（关停）→ 更新 status=3
```

### 15.4 错误码

| 错误码 | 说明 |
|---|---|
| `60010001` | 商家不存在 |
| `60010002` | 店铺名称已存在 |
| `60010003` | 商家已通过审核 |
| `60010004` | 商家不在待审核状态 |
| `60010005` | 无效的审核操作 |

---

## 16. 已知问题与已修复项

### 已修复

| 问题 | 根因 | 修复 |
|---|---|---|
| `MerchantApprovedMessage` 反序列化失败 | `@Data` + `@AllArgsConstructor` 缺少无参构造器, Jackson 2.20.1 无法反序列化 | 所有 5 个 MQ DTO 添加 `@NoArgsConstructor` |
| `product-created` 消费者不工作 | 3 个 inventory 消费者共用 `ecommerce-inventory-v2` group，订阅互相覆盖 | 改为独立 group：`-deduct` / `-release` / `-init` |
| 服务启动时 Topic 不存在，消费者订阅失败 | `autoCreateTopicEnable` 只在 Producer 发消息时建 Topic，Consumer 启动时订阅失败且不重试 | 新增 `docker/rocketmq/init-topics.sh`，服务启动前预创建所有 Topic |

---

## 附录：备忘信息

| 项目 | 信息 |
|---|---|
| MySQL root 密码 | root |
| Redis 密码 | root |
| MinIO 凭证 | minio / minioadmin |
| JWT 签名密钥 | 配置于 application.yml（建议 RSA256） |
| 端口范围 | 8080-8091（后端服务） |
| 管理后台端口 | 5173 (Vite dev) |
| PC 用户端端口 | 3000 (Nuxt dev) |
