# Task Plan: P1 交易闭环 + 商家体系

## Goal
构建京东模式电商平台的交易闭环和商家体系：
- 商家入驻与审核（平台后台管理）
- 购物车服务（Redis 缓存）
- 订单服务（状态流转）
- 支付服务（微信支付）
- PC 用户端（Vue 3 + Nuxt 3 SSR）
- 多角色体系（平台管理员 / 商家 / C端用户）

## Architecture
```
P1 新增:
  ecommerce-cart      (8086) - 购物车 CRUD、Redis、未登录合并
  ecommerce-order     (8084) - 下单/订单状态流转/查询
  ecommerce-payment   (8085) - 微信支付/回调/退款

P2 SQL 提前至 P1:
  ecommerce-merchant  (8087) - 商家入驻/审核/管理
  admin_user.type 扩展 + spu.merchant_id 扩展

前端:
  ecommerce-admin (改造) - 平台管理后台 + 商家后台入口
  ecommerce-web   (新建) - Nuxt3 PC 用户端
```

## Current Phase
Phase 1 (complete)

## Phases

### Phase 0: 数据库迁移（商家体系）
- [x] 执行 P2-multi-role.sql：扩建 admin_user、新建 ecommerce_merchant 库、扩展 spu 表
- [x] 验证迁移结果
- **Status:** complete

### Phase 1: ecommerce-merchant 服务（商家入驻+审核）
- [x] 创建 Maven 模块（端口 8087，错误码前缀 60xx）
- [x] 实体：Merchant、MerchantAudit
- [x] API：POST 入驻申请、GET 商家列表（平台）、PUT 审核（通过/驳回/关停）
- [x] 权限：平台管理员才能审核
- **Status:** complete

### Phase 2: 平台管理后台改造
- [x] 改造为平台视角：商家管理（入驻+审核）为一级导航
- [x] 保留商品/库存（自营运营）为二级导航
- [x] 新增：商家列表页（状态筛选 + 审核弹窗 + 关停确认）
- [x] 新增：商家详情页（信息展示 + 快捷审核）
- [x] 导航重构：商家管理 / 商品运营 / 用户
- [x] Dashboard：入驻商家 / 待审核 / 平台商品 / 注册用户
- **Status:** complete

### Phase 3: ecommerce-cart 服务（购物车）
- [x] 创建 Maven 模块（端口 8086，错误码前缀 35xx）
- [x] Redis Hash 存储：key=cart:user:{userId}, field={skuId}, value=CartItem JSON
- [x] API：GET /cart | POST /cart/items | PUT /cart/items/{skuId} | DELETE /cart/items/{skuId} | DELETE /cart | GET /cart/count | PUT /cart/items/{skuId}/check | POST /cart/merge
- [x] 数量叠加逻辑：同一 SKU 再次添加时 quantity 递增
- [x] 未登录 → 登录：POST /cart/merge 合并匿名购物车（匿名字段增量合并，合并后清除匿名数据）
- [x] 30 天 TTL 自动过期
- [x] Gateway 路由配置
- **Status:** complete

### Phase 4: ecommerce-order 服务（订单）
- [x] 创建 Maven 模块（端口 8084，错误码前缀 40xx）
- [x] 实体：Order (orderNo/userId/totalAmount/status/receiver) + OrderItem (skuId/spuId/name/price/qty/totalPrice)
- [x] 订单状态：0=待支付 1=已支付 2=已发货 3=已完成 4=已取消
- [x] API：POST /orders | GET /orders | GET /orders/:id | PUT /orders/:id/cancel
- [x] 管理端：GET /admin/orders | PUT /admin/orders/:id/ship
- [ ] 库存扣减（待 OpenFeign 集成 ecommerce-inventory）
- [ ] 订单超时取消（待 RocketMQ 集成）
- **Status:** pending

### Phase 5: ecommerce-payment 服务（支付）
- [ ] 创建 Maven 模块（端口 8085，错误码前缀 50xx）
- [ ] 微信支付 JSAPI 对接
- [ ] 支付回调处理
- [ ] 退款流程
- [ ] 对账日志
- **Status:** pending

### Phase 6: PC 用户端（Nuxt 3）
- [x] 创建 Nuxt 3 项目（ecommerce-web）
- [x] 首页（Hero + 商品网格 + 搜索）
- [x] 商品列表页（搜索/分类筛选/分页）
- [x] 商品详情页（SKU 选择 + 加购）
- [x] 购物车页（数量/选中/删除/结算）
- [x] 订单确认页（收货信息 + 提交）
- [x] 登录/注册页
- [x] 个人中心（信息 + 订单列表）
- **Status:** complete

### Phase 7: 验证与集成
- [x] mvn compile 全量通过 (12/12 modules)
- [x] mvn test 全量通过 (92 unit tests, 0 failures)
- [x] Gateway 路由全量验证 (9 routes → 9 services)
- [x] 端口一致性验证 (8080~8087)
- [ ] 买家购物全流程 E2E 测试（注册→搜索→加购→下单→支付，需服务启动）
- **Status:** complete
