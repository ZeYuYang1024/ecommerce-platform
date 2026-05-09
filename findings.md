# Findings & Decisions — P1 阶段

## P1 范围界定

| 维度 | P0 (已完成) | P1 (计划中) |
|------|-----------|-----------|
| 服务数 | 6 + Gateway + Common | 新增 4 个：merchant/cart/order/payment |
| 角色 | 管理员 + C 端用户 | 新增：平台管理员 / 商家 |
| 管理后台 | 单商户自营 | 平台后台 + 商家后台（入口） |
| 前端 | Vue 3 管理后台 | 新增：Nuxt 3 PC 用户端 |
| 交易 | 无 | 购物车 → 下单 → 支付全流程 |

## 关键架构决策

| 决策 | 理由 |
|------|------|
| P2 多角色 SQL 提前至 P1 | 商家入驻是交易闭环的前提 |
| merchant 独立库 ecommerce_merchant | 服务间数据隔离 |
| spu.merchant_id NULL=自营 | 兼容P0数据，无需迁移 |
| 购物车用 Redis 存储 | 高性能读写，支持过期清理 |
| 订单超时用 RocketMQ 延时消息 | 无需定时任务轮询 |
| 支付对接微信 JSAPI | PC 端统一使用 JSAPI 支付 |
| PC 端用 Nuxt 3 SSR | SEO + 首屏性能 |

## 数据库设计（新增/变更）

### ecommerce_merchant 库
- `merchant` 表：店铺名、Logo、联系人、营业执照 URL、状态(待审核/通过/驳回/关停)
- `merchant_audit` 表：审核记录（审核人、动作、意见）

### ecommerce_auth 库变更
- `admin_user` 新增 `type` 字段：super_admin / ops / merchant
- `admin_user` 新增 `merchant_id` 字段

### ecommerce_product 库变更
- `spu` 新增 `merchant_id` 字段：NULL=自营，有值=商家商品

## 微服务清单（P1新增）

| 服务 | 端口 | 错误码前缀 | 职责 |
|------|------|-----------|------|
| ecommerce-merchant | 8087 | 60xx | 商家入驻/审核/管理 |
| ecommerce-cart | 8086 | 35xx | 购物车 Redis CRUD |
| ecommerce-order | 8084 | 40xx | 订单创建/状态流转 |
| ecommerce-payment | 8085 | 50xx | 微信支付/回调/退款 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| P2 多角色 SQL 是否提前至 P1 | 是，商家体系是交易的基础 |

## Resources
- 架构设计：docs/specs/2026-05-08-ecommerce-platform-design.md
- 多角色 SQL：docs/P2-multi-role.sql
- P0 服务清单：ecommerce-gateway/..., ecommerce-auth/..., etc.
