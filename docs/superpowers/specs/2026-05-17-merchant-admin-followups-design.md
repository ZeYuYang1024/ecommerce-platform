# Merchant Admin Follow-Ups Design

## Goal

修复商家账号登录后台后的 9 个后续问题，并把商品、评论、店铺信息三条链路继续按 `merchant` 前缀收口，避免商家态继续误用平台接口。

本次目标包含：

1. 修复商家商品列表和 dashboard 请求平台商品接口导致的 `403`
2. 修复商家商品页“新增商品”跳回 dashboard 的错误跳转
3. 修复商家店铺信息页空白、无法编辑的问题
4. 修复商家结算页当前的布局和信息展示问题
5. 修复商家支付页时间直接显示 ISO 字符串、带 `T` 的问题
6. 修复商家订单页时间展示和表格布局问题
7. 修复商家评论接口 `/api/v1/admin/merchant/reviews` 的 `404`
8. 修复商家品牌页时间和表格样式问题
9. 把商家商品链路统一切到 `/api/v1/admin/merchant/products...`

## Current Findings

- 商家前端页面仍有多处直接请求平台商品接口 `/api/v1/admin/products...`，而网关鉴权只允许商家访问 `/api/v1/admin/merchant/**`，因此在商家态会直接 `403`。
- 商品后端当前已经支持基于 `X-User-Type` 和 `X-Merchant-Id` 的商家数据隔离逻辑，但没有完整暴露为显式的 `/api/v1/admin/merchant/products...` 路径。
- 商家评论后端接口已经存在，但网关路由未纳入 `/api/v1/admin/merchant/reviews/**`，因此前端访问时返回 `404`。
- 商家店铺详情页依赖 `localStorage.merchantId`，但登录响应和登录落盘逻辑当前没有保存 `merchantId`，导致页面拿不到商家主体信息。
- 订单、支付、知识库、店铺等页面直接渲染后端时间字符串，导致界面上出现 `2026-05-16T23:55:00` 这类原始值。
- 结算、订单、品牌等商家页沿用了平台页的表格宽度分配，当前数据量不大时容易出现留白过大、日期换行、状态列拥挤的问题。

## Scope

### In Scope

- 为商品后台补齐显式的商家接口路径：
  - `GET /api/v1/admin/merchant/products`
  - `POST /api/v1/admin/merchant/products`
  - `PUT /api/v1/admin/merchant/products/{id}`
  - `PUT /api/v1/admin/merchant/products/{id}/status`
  - `DELETE /api/v1/admin/merchant/products/{id}`
- 为网关补齐商家商品和商家评论路由放行：
  - `/api/v1/admin/merchant/products/**`
  - `/api/v1/admin/merchant/reviews/**`
- 调整商家端商品列表、dashboard、商品表单、评论页，统一切换到 merchant 前缀
- 登录响应补齐 `merchantId`，前端登录后保存并供商家店铺页读取
- 统一修复订单、支付、知识库、店铺、品牌等页面的时间格式展示
- 调整商家结算、订单、品牌等页面的表格列宽、留白和状态展示密度
- 增加必要的后端单测和前端回归用例，覆盖商品 merchant 路径、评论 merchant 路径和登录态商家页面行为

### Out of Scope

- 不重做整套商家后台视觉体系，只修当前已暴露的布局和格式问题
- 不修改平台后台的路由约定和平台商品页行为
- 不新增新的商品业务字段或商品审核流程
- 不调整商家认证、登录鉴权模型本身
- 不处理本轮问题之外的全局乱码、文案或样式统一工作

## Recommended Approach

采用“接口边界收口 + 前端商家态改走 merchant 路径 + 展示层统一格式化”的方案。

核心原则：

- 商家前端页面只请求 `/api/v1/admin/merchant/**`
- 网关对商家账号继续只放行 merchant 前缀，避免权限边界回退
- 后端商品能力继续复用现有服务层逻辑，但新增显式的 merchant controller 路径或等价映射
- 商品表单继续复用现有平台组件，不复制第二套页面，只增加 merchant 模式分支
- 时间格式问题统一在前端做轻量格式化，不把这类展示规则下沉到后端
- UI 调整只围绕可读性和布局稳定性，不做超出需求的重构

## API and Routing Design

### Merchant Products

当前商品能力已经存在，但路径仍以平台接口为主。本次补齐显式商家路径，避免商家页面继续请求平台地址。

建议提供以下接口：

- `GET /api/v1/admin/merchant/products`
- `POST /api/v1/admin/merchant/products`
- `PUT /api/v1/admin/merchant/products/{id}`
- `PUT /api/v1/admin/merchant/products/{id}/status`
- `DELETE /api/v1/admin/merchant/products/{id}`

实现方式要求：

- 复用现有商品服务层的商家隔离逻辑
- 商家请求的 `merchantId` 来源仍以网关透传的请求头为准
- 保持平台商品接口不变，避免影响现有平台页

### Merchant Reviews

评论后端接口已存在，缺的是网关放行和前端回归验证。

本次要求确认并串通：

- `GET /api/v1/admin/merchant/reviews`
- `DELETE /api/v1/admin/merchant/reviews/{id}`

网关需把该路径纳入商品服务路由转发范围。

### Merchant Shop Data

商家店铺详情继续复用现有商家详情接口，不新增 merchant 专用详情接口：

- `GET /api/v1/admin/merchants/{id}`
- `PUT /api/v1/admin/merchants/{id}`

本次只补登录上下文：

- 登录响应增加 `merchantId`
- 前端登录成功后保存 `merchantId`
- 商家店铺页使用该值拉取和更新当前店铺信息

## Frontend Behavior Design

### Merchant Product Pages

商家商品相关页面统一进入 merchant 模式。

涉及页面：

- `merchant/products.vue`
- `merchant/dashboard.vue`
- `product/form.vue`

行为调整：

- 商品列表请求改为 `/api/v1/admin/merchant/products`
- dashboard 中商品数量或商品示例查询改为 `/api/v1/admin/merchant/products?page=1&size=1`
- “新增商品”按钮跳转改为商家专用路径，而不是平台商品创建页
- 编辑商品从商家列表进入时，也走 merchant 专用编辑路径
- 商品表单提交成功后返回商家商品列表
- 商品表单在 merchant 模式下：
  - 创建、编辑、上下架、删除全部走 merchant 接口
  - 品牌下拉只加载商家可用品牌

### Merchant Login Context

登录成功后前端需稳定保存以下上下文：

- `token`
- `username`
- `type`
- `merchantId`

其中 `merchantId` 既用于店铺信息页面，也为后续商家态页面进一步去平台化提供基础。

### Time Formatting

前端新增统一的轻量时间格式化方法，处理 `LocalDateTime` 原始字符串。

格式规则：

- 输入：`2026-05-16T23:55:00`
- 输出：`2026-05-16 23:55:00`

优先覆盖页面：

- 商家订单页
- 商家支付页
- 商家对账页
- 商家知识库文档页
- 商家店铺信息页
- 品牌页中所有需要展示时间的字段

### UI Layout Adjustments

本次只做局部布局调整，目标是让表格在真实商家数据下可读、稳定、不过度留白。

重点页面：

- 结算页
- 订单页
- 品牌页

调整原则：

- 重新分配日期、金额、状态、操作列宽
- 避免订单时间、品牌信息、收货人被不必要地换行
- 压缩右侧大面积空白
- 保持状态标签、操作按钮在同一视觉节奏下展示
- 不改变当前整体配色和卡片式页面结构

## File-Level Design

### Backend

- `ecommerce-product`
  - 新增 merchant 商品 controller，或在现有 controller 上补显式 merchant 路由映射
  - 保持服务层商家隔离逻辑不变
- `ecommerce-auth`
  - 登录响应 DTO 增加 `merchantId`
  - 登录服务组装响应时写入 `merchantId`
- `ecommerce-gateway`
  - 补齐 merchant products / reviews 路由配置
  - 保持商家账号只允许访问 merchant 前缀的鉴权原则

### Frontend

- `ecommerce-admin/src/views/merchant/products.vue`
  - 改用 merchant products 接口
  - 修正新增、编辑入口跳转
- `ecommerce-admin/src/views/merchant/dashboard.vue`
  - 改用 merchant products 接口获取商品摘要
- `ecommerce-admin/src/views/product/form.vue`
  - 增加 merchant 模式识别
  - 根据模式切换请求地址和提交后跳转地址
- `ecommerce-admin/src/views/login/index.vue`
  - 保存 `merchantId`
- `ecommerce-admin/src/views/merchant/shop.vue`
  - 使用 `merchantId` 拉取和更新店铺资料
- `ecommerce-admin/src/views/review/index.vue`
  - 保持 merchant reviews 接口调用，配合网关放行
- `ecommerce-admin/src/views/payment/list.vue`
  - 格式化支付时间
- `ecommerce-admin/src/views/merchant/orders.vue`
  - 格式化订单时间
  - 调整列布局
- `ecommerce-admin/src/views/settlement/index.vue`
  - 调整结算页表格和状态展示布局
- `ecommerce-admin/src/views/brand/index.vue`
  - 调整品牌页时间和表格样式

## Testing Strategy

### Backend Tests

- 商品 controller 增加 merchant 路径测试，验证：
  - 列表只返回当前商家数据
  - 创建时写入当前商家归属
  - 编辑、上下架、删除不会越权到其他商家
- 登录响应测试验证 `merchantId` 已返回
- 网关测试验证：
  - 商家账号可访问 `/api/v1/admin/merchant/products/**`
  - 商家账号可访问 `/api/v1/admin/merchant/reviews/**`
  - 商家账号继续不可访问平台 `/api/v1/admin/products/**`

### Frontend Verification

至少覆盖以下真实行为：

1. 商家登录后进入商品页，请求不再出现平台商品接口 `403`
2. 商家商品页点击“新增商品”进入正确表单页，不再回 dashboard
3. 商家店铺信息页可以拉到已有店铺数据并保存修改
4. 商家评论页接口不再 `404`
5. 支付、订单等页面时间展示不再带 `T`
6. 结算、订单、品牌页在真实数据下无明显列错位和大块空白

如继续保留 Playwright 真实联调用例，可为以下页面补最小回归：

- merchant products
- merchant reviews
- merchant shop
- merchant orders
- payment list

## Risks and Mitigations

- 商品表单当前是平台页复用组件，直接改动可能影响平台管理页
  - 通过明确的 merchant 模式判断隔离请求地址和跳转地址
  - 平台模式默认行为保持不变
- 登录响应增加字段可能影响现有前端解析
  - 采用向后兼容字段追加方式，不修改已有字段含义
- 网关补 merchant 路由时可能出现路径重叠或转发顺序问题
  - 补充针对 merchant products / reviews 的专门路由测试
- UI 调整容易演变成大范围视觉改造
  - 仅修改列宽、留白、时间文本和局部排版，不新增新的视觉系统

## Acceptance Criteria

- 商家登录访问商品相关页面时，不再出现 `/api/v1/admin/products...` 的 `403`
- 商家商品页“新增商品”进入商家商品表单，并能正常返回商家商品列表
- 商家店铺信息页能展示并编辑当前店铺数据
- 商家评论列表接口返回 `200`，页面可正常展示
- 支付、订单、知识库、店铺等页面显示的时间不再带 `T`
- 结算、订单、品牌页在现有测试数据下布局稳定、无明显错位和过度留白
- 商家商品、评论相关前端调用全部切换为 `/api/v1/admin/merchant/**`
- 网关仍保持商家账号不能直接访问平台商品后台接口
