# Spring Boot Admin 集成设计

**日期**: 2026-05-13  
**状态**: 已批准

## 目标

引入 Spring Boot Admin 4.0.4，集中监控 15 个微服务的健康状态、性能指标、Druid 数据源，替代逐服务查看 Druid 页面的方式。

## 架构

```
Nacos Registry ←── 所有服务注册
       │
       ▼ (自动发现)
ecommerce-monitor (:8090)
  └── SBA Server + UI (/admin)
       │
       ▼ (拉取 actuator)
auth / user / product / inventory / merchant
order / payment / coupon / seckill / notification
cart / file / gateway / search / common
```

- SBA Server 通过 Nacos 自动发现所有服务，无需手动配置 URL
- 每个服务通过 actuator 暴露运行指标，SBA Server 定时拉取
- 监控 UI 挂载在 `/admin`，访问 `http://localhost:8090/admin`

## 新增模块

### ecommerce-monitor

- 独立 Spring Boot 应用，端口 8090
- 依赖：`spring-boot-admin-starter-server` 4.0.4
- 依赖：`spring-cloud-starter-alibaba-nacos-discovery`（自动发现）
- 注解：`@EnableAdminServer` + `@SpringBootApplication`

## 现有模块变更

### 15 个服务 pom.xml（每个加 2 个依赖）

- `spring-boot-admin-starter-client` 4.0.4
- `spring-boot-starter-actuator`（版本由 parent 管理）

### 15 个 application.yml（每个加 management 配置）

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
```

### Root pom.xml

- `<druid.version>` → 已有
- 新增 `<spring-boot-admin.version>4.0.4</spring-boot-admin.version>`
- dependencyManagement 中加 server 和 client 的版本管理

### Gateway（可选）

- 加路由 `/admin/**` → `lb://ecommerce-monitor`，通过网关也能访问 SBA UI

## 影响的服务列表

所有 15 个模块：

| 模块 | 端口 | 类型 |
|------|------|------|
| ecommerce-gateway | 8080 | WebFlux 网关 |
| ecommerce-auth | 8091 | WebMVC |
| ecommerce-user | 8081 | WebMVC |
| ecommerce-product | 8082 | WebMVC |
| ecommerce-inventory | 8083 | WebMVC |
| ecommerce-order | 8084 | WebMVC |
| ecommerce-payment | 8085 | WebMVC |
| ecommerce-merchant | 8087 | WebMVC |
| ecommerce-coupon | 8088 | WebMVC |
| ecommerce-notification | 8089 | WebMVC |
| ecommerce-seckill | 8093 | WebMVC |
| ecommerce-cart | - | WebMVC (Redis) |
| ecommerce-file | - | WebMVC (MinIO) |
| ecommerce-search | - | WebMVC (ES) |
| ecommerce-common | - | 库模块（无需） |

> ecommerce-common 是纯库模块，没有 main 类和 application.yml，不加 actuator。

## SBA 功能

- 服务健康状态总览（UP/DOWN）
- 内存 / CPU / 线程 / GC 实时指标
- Druid 数据源指标（连接池、慢查询）集中展示
- 日志查看 + 运行时修改日志级别
- 环境变量 / 配置查看
- Spring Cloud Gateway 路由查看
- Wallboard 大屏模式

## 不包含

- SBA Server 登录认证（后续按需加 Spring Security）
- 告警通知（后续按需加邮件/钉钉）
- Prometheus / Grafana 集成（后续按需）

## 验证方式

1. 启动 ecommerce-monitor
2. 启动若干业务服务（如 auth, product）
3. 访问 `http://localhost:8090/admin`，确认所有服务出现在 Wallboard
4. 点击服务查看详情（健康、指标、日志）
5. 确认 Druid 数据源指标可见
