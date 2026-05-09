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
- [ ] 改造为平台视角：商家管理取代商品管理
- [ ] 删除或迁移：商品 CRUD → 未来商家后台
- [ ] 新增：商家入驻审核页面
- [ ] 导航重构：商家管理 / 类目管理 / 用户管理
- **Status:** pending

### Phase 3: ecommerce-cart 服务（购物车）
- [ ] 创建 Maven 模块（端口 8086，错误码前缀 35xx）
- [ ] Redis 存储购物车数据
- [ ] API：ADD/DELETE/UPDATE/CLEAR/LIST/COUNT
- [ ] 未登录 → 登录：购物车合并
- [ ] Gateway 路由配置
- **Status:** pending

### Phase 4: ecommerce-order 服务（订单）
- [ ] 创建 Maven 模块（端口 8084，错误码前缀 40xx）
- [ ] 实体：Order、OrderItem
- [ ] 流程：下单 → 待支付 → 已支付 → 已发货 → 已完成 → 已取消
- [ ] 库存扣减（调用 ecommerce-inventory）
- [ ] 订单超时取消（RocketMQ 延时消息）
- **Status:** pending

### Phase 5: ecommerce-payment 服务（支付）
- [ ] 创建 Maven 模块（端口 8085，错误码前缀 50xx）
- [ ] 微信支付 JSAPI 对接
- [ ] 支付回调处理
- [ ] 退款流程
- [ ] 对账日志
- **Status:** pending

### Phase 6: PC 用户端（Nuxt 3）
- [ ] 创建 Nuxt 3 项目（ecommerce-web）
- [ ] 首页（商品搜索/分类导航/推荐）
- [ ] 商品详情页
- [ ] 购物车页
- [ ] 订单确认页
- [ ] 个人中心（订单/地址/信息）
- **Status:** pending

### Phase 7: 验证与集成
- [ ] mvn compile 全量通过
- [ ] 所有服务 Nacos 注册成功
- [ ] Gateway 路由全量验证
- [ ] 买家购物全流程 E2E 测试（注册→搜索→加购→下单→支付）
- **Status:** pending
