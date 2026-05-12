# 微信小程序 C 端商城设计文档

> 日期：2026-05-12
> 状态：待审核
> 技术栈：uni-app (Vue 3) + Pinia + uni-ui

---

## 1. 项目概述

为微服务电商平台构建微信小程序 C 端商城，面向消费者提供完整的购物体验。后端 API 全部复用现有微服务，小程序只做前端展示和交互。

### 边界决策

| 决策 | 理由 |
|------|------|
| 只做 C 端，不做管理端 | 管理后台已有 ecommerce-admin (Vue 3 Web)，手机上不适合管理操作 |
| API 零新建 | PC 用户端已调通的 11 个 API 路径直接复用 |
| 不做独立后端 | Gateway 鉴权 + 现有微服务全部可用 |

---

## 2. 技术架构

```
┌─────────────────────────────────────────┐
│           uni-app (Vue 3)               │
│  ┌─────────┐ ┌──────────┐ ┌─────────┐  │
│  │  Pages  │ │Components│ │ Stores  │  │
│  │ 10 pages│ │  goods   │ │  auth   │  │
│  │         │ │  coupon  │ │  cart   │  │
│  │         │ │  order   │ │  user   │  │
│  └─────────┘ └──────────┘ └─────────┘  │
│         │                               │
│         ▼  wx.request / uni.request     │
├─────────────────────────────────────────┤
│      Spring Cloud Gateway :8080         │
│      (JWT 鉴权 + 角色路由)              │
│      /api/v1/*  →  各微服务             │
└─────────────────────────────────────────┘
```

### 依赖的现有 API

| 模块 | API 路径 | 用途 |
|------|----------|------|
| 商品 | `/api/v1/products` `/.../categories` | 首页/分类/详情 |
| 搜索 | `/api/v1/search` | 搜索页 |
| 购物车 | `/api/v1/cart` | 购物车 CRUD |
| 订单 | `/api/v1/orders` | 下单/我的订单/详情 |
| 优惠券 | `/api/v1/coupons` | 领券/我的券/核销 |
| 秒杀 | `/api/v1/seckill` | 场次/秒杀下单 |
| 用户 | `/api/v1/users` | 地址/个人信息 |
| 认证 | `/api/v1/auth/login` `/.../register` | 登录注册 |
| 文件 | `/api/v1/files` | 图片上传(如有) |

### 状态管理

```
stores/
├── auth.js    — token, userInfo, isLogin, login(), logout()
├── cart.js    — cartItems, cartCount, addToCart(), removeFromCart()
└── user.js    — profile, addresses
```

---

## 3. 页面清单

### 3.1 首页 `pages/index/index`

**布局：**
- 顶部搜索栏（点击跳搜索页）
- 轮播 Banner（3-5 张）
- 分类 icon 入口（4-8 个图标网格）
- "限时秒杀"横向滑动卡片（进行中的秒杀场次 + 商品）
- "热门商品"瀑布流/双列网格

**数据源：** `GET /products?page=1&size=10`, `GET /seckill/sessions`, `GET /categories`

---

### 3.2 搜索页 `pages/search/index`

**布局：**
- 顶部搜索输入框（自动聚焦）
- 分类下拉 + 排序选择
- 搜索结果双列网格
- 分页加载（触底加载更多）

**数据源：** `GET /search?keyword=&categoryId=&sort=&page=&size=`

---

### 3.3 商品详情 `pages/product/detail`

**布局：**
- 顶部商品大图轮播（swiper）
- 价格区：秒杀价/原价（划线）
- 商品名称 + 描述
- 规格选择（如有 SKU）
- 底部固定栏：购物车 icon + "加入购物车" + "立即购买"
- "立即购买" → 跳转下单页

**数据源：** `GET /products/:id`

---

### 3.4 分类页 `pages/category/index`

**布局：**
- 左侧一级分类列表
- 右侧二级分类 + 商品网格
- 点击商品跳详情

**数据源：** `GET /categories`

---

### 3.5 购物车 `pages/cart/index`

**布局：**
- 商品列表（勾选框 + 图 + 名称 + 单价 + 数量 +/-）
- 全选/取消全选
- 底部合计价 + "去结算"按钮
- 空购物车占位图

**数据源：** `GET /cart`, `PUT /cart/:id`, `DELETE /cart/:id`

---

### 3.6 下单结算 `pages/checkout/index`

**布局：**
- 收货地址选择（可新增/编辑）
- 商品清单（只读）
- 优惠券选择（可用券列表 → 选择 → 计算折扣）
- 订单金额明细（商品总额 - 优惠 + 运费）
- "提交订单"按钮

**数据源：** `GET /users/addresses`, `GET /coupons?status=0`, `POST /coupons/verify`, `POST /orders`

---

### 3.7 我的订单 `pages/order/list`

**布局：**
- Tab 切换：全部/待付款/待发货/待收货/已完成
- 订单卡片：订单号 + 商品缩略图 + 金额 + 状态标签
- 点击跳订单详情

**数据源：** `GET /orders`

---

### 3.8 订单详情 `pages/order/detail`

**布局：**
- 订单状态进度条
- 收货信息
- 商品清单
- 金额明细（含优惠券折扣）
- 操作按钮：去支付/取消订单/确认收货

**数据源：** `GET /orders/:id` / `GET /orders/no/:orderNo`

---

### 3.9 优惠券中心 `pages/coupon/index`

**布局：**
- Tab：可领取 / 我的券
- 可领取：券卡片 + "立即领取"按钮
- 我的券：已领券列表，标注状态（可用/已用/过期）
- 点击可用券 → 跳商品列表（按条件筛选）

**数据源：** `GET /coupons`, `POST /coupons/claim`

---

### 3.10 秒杀会场 `pages/seckill/index`

**布局：**
- 顶部场次倒计时
- 秒杀商品卡片（原价划线 + 秒杀价 + 进度条 + "立即秒杀"按钮）
- 倒计时结束显示"已结束"

**数据源：** `GET /seckill/sessions`, `GET /seckill/items`, `POST /seckill/order`

---

### 3.11 用户中心 `pages/user/index`

**布局：**
- 头像 + 昵称区
- 我的订单入口（待付款/待发货/待收货 badge）
- 功能列表：优惠券 / 收货地址 / 设置
- 退出登录

---

## 4. UI 设计规范

### 4.1 色彩

| 用途 | 色值 | 说明 |
|------|------|------|
| 主色 | `#F59E0B` (amber-500) | 按钮、标签、强调 |
| 主色深 | `#D97706` (amber-600) | hover/active 态 |
| 渐变 | `#F59E0B → #F97316` | 大按钮、Banner |
| 背景 | `#F8F8F8` | 页面背景 |
| 卡片 | `#FFFFFF` | 内容卡片 |
| 文字主 | `#1F2937` (gray-800) | 标题、正文 |
| 文字辅 | `#9CA3AF` (gray-400) | 描述、时间 |
| 红色 | `#EF4444` | 秒杀价、倒计时 |
| 绿色 | `#10B981` | 成功状态 |

### 4.2 圆角与间距

- 卡片圆角：16rpx（小卡）/ 20rpx（大卡）
- 按钮圆角：12rpx
- 页面边距：24rpx
- 卡片间距：16rpx
- 图标尺寸：40rpx（小）/ 80rpx（中）/ 120rpx（大）

### 4.3 字体

- 标题：34rpx bold
- 正文：28rpx
- 辅助文字：24rpx
- 价格数字：36rpx bold（主色）/ 24rpx（划线原价）
- 按钮文字：28rpx medium

### 4.4 组件

- **商品卡片**：16:12 商品图 + 名称(2行截断) + 价格行 + 销量/评分
- **券卡片**：左侧金额 + 右侧条件 + "领取"按钮
- **订单卡片**：订单号行 + 商品缩略图 + 金额 + 状态标签
- **秒杀卡片**：倒计时条 + 商品图 + 秒杀价 + 进度条

### 4.5 底部 TabBar

| Tab | icon | 页面 |
|-----|------|------|
| 首页 | home | `/pages/index/index` |
| 分类 | grid | `/pages/category/index` |
| 购物车 | cart (+badge) | `/pages/cart/index` |
| 我的 | user | `/pages/user/index` |

---

## 5. 关键交互流程

### 5.1 下单完整链路

```
商品详情 → 加入购物车 → 购物车 → 去结算
    │                        │
    └── 立即购买 ─────────────┘
                  │
                  ▼
            选择/确认地址
                  │
                  ▼
            选择优惠券(可选)
                  │
                  ▼
            确认金额 → 提交订单
                  │
                  ▼
            跳转支付(微信支付)
                  │
                  ▼
            支付成功 → 订单详情
```

### 5.2 秒杀流程

```
秒杀会场 → 倒计时进行中
    │
    ▼
点击"立即秒杀"
    │
    ├── 未登录 → 跳登录
    │
    ▼
POST /seckill/order
    ├── 成功 → 提示"抢购成功" → 跳订单详情
    └── 失败 → 提示原因（库存不足/已结束）
```

### 5.3 领券流程

```
优惠券中心 → 可领取列表
    │
    ▼
点击"立即领取"
    │
    ├── 未登录 → 跳登录
    │
    ▼
POST /coupons/claim?templateId=xxx
    ├── 成功 → 券状态变为"已领取"
    └── 失败 → 提示原因（已领完/已达上限）
```

---

## 6. 错误处理

- **网络异常**：统一 toast "网络开小差了，请重试"
- **未登录**：自动弹出登录页（微信一键登录 wx.login）
- **库存不足**：toast "商品已售罄"
- **优惠券不可用**：toast 具体原因（未达门槛/已过期等）
- **秒杀失败**：toast "手慢了，已抢光"

---

## 7. 文件结构

```
ecommerce-miniprogram/
├── pages/
│   ├── index/         首页
│   ├── search/        搜索
│   ├── product/       商品详情
│   ├── category/      分类
│   ├── cart/          购物车
│   ├── checkout/      下单
│   ├── order/         订单列表+详情
│   ├── coupon/        优惠券
│   ├── seckill/       秒杀
│   └── user/          用户中心
├── components/
│   ├── ProductCard.vue
│   ├── CouponCard.vue
│   ├── OrderCard.vue
│   ├── SeckillCard.vue
│   └── AppNavbar.vue
├── stores/
│   ├── auth.js
│   ├── cart.js
│   └── user.js
├── utils/
│   ├── api.js         uni.request 封装
│   └── auth.js        token 管理
├── static/            icon + 占位图
├── App.vue
├── main.js
├── pages.json         TabBar + 路由配置
├── manifest.json      微信小程序配置
└── uni.scss           全局样式变量
```

---

## 8. 自检清单

- [x] 所有页面有明确数据源
- [x] API 全部复用现有，无新建
- [x] 3 条核心交互流程已明确
- [x] UI 规范覆盖色彩/圆角/字体/组件
- [x] 错误处理覆盖网络/登录/业务异常
- [x] 状态管理只涉及 auth/cart/user
- [x] 底部 TabBar 4 个 tab 已定义
