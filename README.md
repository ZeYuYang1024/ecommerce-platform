# E-Commerce Platform

微服务电商平台，前后端分离 + 微信小程序，Spring Boot 4.0 + Spring Cloud + Vue 3 + uni-app。

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 4.0.0 | 微服务框架 |
| Spring Cloud | 2025.1.1 | 服务治理 (Gateway / OpenFeign / LoadBalancer) |
| Spring Cloud Alibaba Nacos | 2025.1.0.0 (client) / 2.4.0 (server) | 注册中心 & 配置中心 |
| MyBatis-Plus | 3.5.16 | ORM |
| MySQL | 8.0.33 (driver) / 8.0 (server) | 关系型数据库 |
| Druid | 1.2.28 | 数据库连接池 + SQL 监控 |
| Redis | 7.2 | 缓存 (Lettuce 驱动) |
| RocketMQ | 5.2.0 (server) / 2.3.0 (starter) | 消息队列 |
| Elasticsearch | 7.17.28 (server) / 8.16.0 (client) | 搜索引擎 |
| MinIO | latest (server) / 8.6.0 (SDK) | 对象存储 |
| JJWT | 0.12.6 | JWT 认证 |
| Hutool | 5.8.44 | 工具库 |
| Lombok | 1.18.46 | 代码简化 |
| Spring Boot Admin | 4.0.4 | 集中监控 |

### 前端 (PC Web)

| 技术 | 版本 | 说明 |
|------|------|------|
| Nuxt 3 | ^3.15.0 | SSR 框架 |
| Vue 3 | ^3.5.0 | 组件框架 |
| Pinia | ^2.2.0 | 状态管理 |
| Tailwind CSS | ^3.4.0 | CSS 框架 |
| vue-router | ^4.4.0 | 路由 |

### 前端 (Admin 管理后台)

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | ^3.5.32 | 组件框架 |
| Vite | ^8.0.10 | 构建工具 |
| Element Plus | ^2.13.7 | UI 组件库 |
| Pinia | ^3.0.4 | 状态管理 |
| Axios | ^1.16.0 | HTTP 客户端 |
| Sass | ^1.99.0 | CSS 预处理器 |

### 微信小程序

| 技术 | 版本 | 说明 |
|------|------|------|
| uni-app | Vue 3 模式 | 跨端框架 |
| Vue 3 | ^3.4.0 | 组件框架 |
| Sass | ^1.70.0 | CSS 预处理器 |
| HBuilderX | 最新版 | IDE / 构建工具 |

### 测试

| 技术 | 版本 | 说明 |
|------|------|------|
| Playwright | ^1.59.1 | E2E 测试框架 |
| H2 | 2.3.232 | 测试用内存数据库 |
| miniprogram-automator | ^0.12.1 | 小程序自动化测试 |

## 项目结构

```
ecommerce-platform/
├── ecommerce-common/          # 公共模块 (DTO / Result / 工具类)
├── ecommerce-gateway/         # API 网关 (:8080)  WebFlux
├── ecommerce-monitor/         # SBA 监控 (:8094)
├── ecommerce-auth/            # 认证服务 (:8091)
├── ecommerce-user/            # 用户服务 (:8081)
├── ecommerce-product/         # 商品服务 (:8082)
├── ecommerce-inventory/       # 库存服务 (:8083)
├── ecommerce-order/           # 订单服务 (:8084)
├── ecommerce-payment/         # 支付服务 (:8085)
├── ecommerce-cart/            # 购物车 (:8086)
├── ecommerce-merchant/        # 商家服务 (:8087)
├── ecommerce-coupon/          # 优惠券 (:8088)
├── ecommerce-notification/    # 通知服务 (:8089)
├── ecommerce-file/            # 文件服务 (:8090)
├── ecommerce-search/          # 搜索服务 (:8092)
├── ecommerce-seckill/         # 秒杀服务 (:8093)
├── ecommerce-web/             # PC 前端 (Nuxt 3)  :3000
├── ecommerce-admin/           # 管理后台 (Vite)   :5173
├── ecommerce-miniprogram/     # 微信小程序 (uni-app)
├── docs/
│   └── init.sql               # 完整建库建表 + 测试数据
├── scripts/
│   ├── start-middleware.sh    # Mac/Linux 一键启动脚本
│   └── start-middleware.ps1   # Windows PowerShell 一键启动脚本
└── docker-compose.yml         # 中间件 Docker 编排
```

## 中间件端口

| 中间件 | 端口 | 账号 / 密码 |
|--------|------|------------|
| MySQL | 3306 | root / root |
| Redis | 6379 | 密码: root |
| Nacos | 8848 (http) / 9848 (gRPC) | nacos / nacos |
| RocketMQ NameServer | 9876 | - |
| RocketMQ Broker | 10911 (remoting) / 10909 (VIP) | - |
| MinIO | 9000 (API) / 9001 (Console) | minio / minioadmin |
| Elasticsearch | 9200 | - |

## 快速启动

### 1. 前置条件

- **Docker Desktop** (或 Colima / Podman) — 运行中间件容器
- **JDK 21** — 编译运行后端
- **Maven 3.9+** — 项目构建
- **Node.js 22+** — 前端构建
- **HBuilderX** — 小程序编译 (仅小程序)

### 2. 启动中间件

**macOS / Linux:**
```bash
bash scripts/start-middleware.sh
```

**Windows (PowerShell):**
```powershell
.\scripts\start-middleware.ps1
```

脚本会自动完成：启动 Docker 容器 → 等待服务就绪 → 初始化 RocketMQ Topics → 执行 `docs/init.sql`。

**手动启动（按需）：**
```bash
# 仅启动特定中间件
docker compose up -d mysql redis nacos

# 初始化数据库
docker exec -i ecommerce-mysql mysql -uroot -proot < docs/init.sql

# 初始化 RocketMQ Topics
docker exec ecommerce-rmq-broker ./mqadmin updateTopic \
  -n rocketmq-namesrv:9876 -c DefaultCluster \
  -t order-paid -r 4 -w 4
```

### 3. 启动后端服务

```bash
# 每个模块独立启动
cd ecommerce-auth && mvn spring-boot:run     # :8091
cd ecommerce-product && mvn spring-boot:run  # :8082
# ...

# 或者一条命令编译全部
mvn compile -q
```

建议启动顺序：`common → auth → user → product → inventory → order → payment → gateway → 其他`

### 4. 启动前端

```bash
# PC Web (ecommerce-web)
cd ecommerce-web
npm install
npm run dev          # http://localhost:3000

# 管理后台 (ecommerce-admin)
cd ecommerce-admin
npm install
npm run dev          # http://localhost:5173
```

### 5. 微信小程序

在 HBuilderX 中打开 `ecommerce-miniprogram` 目录，配置 `manifest.json` 中的微信 AppID，点击「运行 → 运行到小程序模拟器 → 微信开发者工具」。

### 6. 访问地址

| 页面 | 地址 |
|------|------|
| PC Web | http://localhost:3000 |
| 管理后台 | http://localhost:5173 |
| API 网关 | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos |
| MinIO 控制台 | http://localhost:9001 |
| **Spring Boot Admin** | http://localhost:8094/admin |
| **Druid SQL 监控** | http://localhost:{服务端口}/druid/sql.html |
| Druid 登录 | admin / admin |

## RocketMQ Topics

| Topic | 队列数 | 说明 |
|-------|--------|------|
| `order-created` | 4 | 订单创建 |
| `order-cancelled` | 4 | 订单取消 → 库存释放 |
| `order-paid` | 4 | 支付成功 → 订单状态更新 |
| `product-created` | 4 | 商品创建 → 同步 ES |
| `merchant-approved` | 4 | 商家审核通过 → 创建管理账号 |

## 数据库

### 10 个业务数据库

每个数据服务独享一个数据库：`ecommerce_auth`, `ecommerce_user`, `ecommerce_product`, `ecommerce_inventory`, `ecommerce_merchant`, `ecommerce_order`, `ecommerce_payment`, `ecommerce_coupon`, `ecommerce_notification`, `ecommerce_seckill`。

### 执行初始化

```bash
docker exec -i ecommerce-mysql mysql -uroot -proot < docs/init.sql
```

`init.sql` 包含：建库、建表、分类/品牌/商品/库存测试数据、RBAC 角色权限、管理员账号 (admin/admin123)、测试用户 (testuser/test123)。

## 监控

### Spring Boot Admin

集中监控所有服务：http://localhost:8094/admin

- 服务健康状态 / 内存 / 线程 / GC
- 日志查看 & 运行时修改日志级别
- 环境变量 / 配置查看
- Wallboard 大屏模式

### Druid SQL 监控

各服务独立监控：http://localhost:{port}/druid/sql.html

- SQL 执行统计 & 慢查询
- 连接池状态
- 登录账号：admin / admin

### Admin 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 超级管理员 | admin | admin123 |
| 测试用户 | testuser | test123 |
