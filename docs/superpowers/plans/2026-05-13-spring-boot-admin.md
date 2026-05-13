# Spring Boot Admin 集成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 ecommerce-monitor 模块 + 所有服务接入 SBA 4.0.4，通过 Nacos 自动发现

**Architecture:** 新建 ecommerce-monitor (8094) 作为 SBA Server，UI 在 /admin；14 个业务服务加 actuator + SBA client 暴露指标；SBA Server 通过 Nacos 自动发现所有服务

**Tech Stack:** Spring Boot Admin 4.0.4, Spring Boot Actuator, Nacos Discovery

---

### Task 1: Root pom.xml — 加版本管理

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 在 root pom.xml 加版本属性和 dependencyManagement**

在 `<properties>` 中 `<druid.version>` 行后加：
```xml
        <spring-boot-admin.version>4.0.4</spring-boot-admin.version>
```

在 `<dependencyManagement>` 中 `<druid-spring-boot-4-starter>` 后加：
```xml
            <dependency>
                <groupId>de.codecentric</groupId>
                <artifactId>spring-boot-admin-starter-server</artifactId>
                <version>${spring-boot-admin.version}</version>
            </dependency>
            <dependency>
                <groupId>de.codecentric</groupId>
                <artifactId>spring-boot-admin-starter-client</artifactId>
                <version>${spring-boot-admin.version}</version>
            </dependency>
```

在 root pom.xml `<modules>` 中加：
```xml
        <module>ecommerce-monitor</module>
```

- [ ] **Step 2: 验证 pom.xml 格式**

Run: `mvn validate -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "chore: root pom 加 Spring Boot Admin 4.0.4 版本管理 + monitor 模块声明"
```

---

### Task 2: 新建 ecommerce-monitor 模块

**Files:**
- Create: `ecommerce-monitor/pom.xml`
- Create: `ecommerce-monitor/src/main/java/com/ecommerce/monitor/MonitorApplication.java`
- Create: `ecommerce-monitor/src/main/resources/application.yml`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>ecommerce-monitor</artifactId>
    <dependencies>
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 MonitorApplication.java**

```java
package com.ecommerce.monitor;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAdminServer
public class MonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
```

- [ ] **Step 3: 创建 application.yml**

```yaml
server:
  port: 8094

spring:
  application:
    name: ecommerce-monitor
  boot:
    admin:
      context-path: /admin
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848

management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
```

- [ ] **Step 4: Commit**

```bash
git add ecommerce-monitor/
git commit -m "feat: 新建 ecommerce-monitor 模块 — SBA Server :8094"
```

---

### Task 3: 14 个服务加 actuator + SBA client 依赖

**Files:**
- Modify: `ecommerce-auth/pom.xml`
- Modify: `ecommerce-user/pom.xml`
- Modify: `ecommerce-product/pom.xml`
- Modify: `ecommerce-inventory/pom.xml`
- Modify: `ecommerce-merchant/pom.xml`
- Modify: `ecommerce-order/pom.xml`
- Modify: `ecommerce-payment/pom.xml`
- Modify: `ecommerce-coupon/pom.xml`
- Modify: `ecommerce-notification/pom.xml`
- Modify: `ecommerce-seckill/pom.xml`
- Modify: `ecommerce-cart/pom.xml`
- Modify: `ecommerce-file/pom.xml`
- Modify: `ecommerce-search/pom.xml`
- Modify: `ecommerce-gateway/pom.xml`

- [ ] **Step 1: 给每个 pom.xml 加 actuator 和 SBA client**

在每个服务 pom.xml 的 `<dependencies>` 尾部（`</dependencies>` 前）加：

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-client</artifactId>
        </dependency>
```

共 14 个文件，格式一致。需要逐一修改。

- [ ] **Step 2: 验证编译**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add ecommerce-*/pom.xml
git commit -m "feat: 14 个服务加 actuator + SBA client 依赖"
```

---

### Task 4: 14 个 application.yml 加 management 配置

**Files:**
- Modify: `ecommerce-auth/src/main/resources/application.yml`
- Modify: `ecommerce-user/src/main/resources/application.yml`
- Modify: `ecommerce-product/src/main/resources/application.yml`
- Modify: `ecommerce-inventory/src/main/resources/application.yml`
- Modify: `ecommerce-merchant/src/main/resources/application.yml`
- Modify: `ecommerce-order/src/main/resources/application.yml`
- Modify: `ecommerce-payment/src/main/resources/application.yml`
- Modify: `ecommerce-coupon/src/main/resources/application.yml`
- Modify: `ecommerce-notification/src/main/resources/application.yml`
- Modify: `ecommerce-seckill/src/main/resources/application.yml`
- Modify: `ecommerce-cart/src/main/resources/application.yml`
- Modify: `ecommerce-file/src/main/resources/application.yml`
- Modify: `ecommerce-search/src/main/resources/application.yml`
- Modify: `ecommerce-gateway/src/main/resources/application.yml`

- [ ] **Step 1: 每个 application.yml 末尾加 management 配置**

在文件末尾追加：

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

共 14 个文件。每个文件末尾追加上面内容（确保与已有配置之间留一个空行）。

- [ ] **Step 2: Commit**

```bash
git add ecommerce-*/src/main/resources/application.yml
git commit -m "feat: 14 个服务加 management 端点暴露配置"
```

---

### Task 5: Gateway 路由（可选）

**Files:**
- Modify: `ecommerce-gateway/src/main/resources/application.yml`

- [ ] **Step 1: 网关加 SBA UI 路由**

在 gateway routes 中加：
```yaml
            - id: monitor
              uri: lb://ecommerce-monitor
              predicates:
                - Path=/admin/**
```

加在其他 route 之前或之后均可。这样可以 `http://localhost:8080/admin` 通过网关访问 SBA UI。

- [ ] **Step 2: Commit**

```bash
git add ecommerce-gateway/src/main/resources/application.yml
git commit -m "feat: 网关加 /admin 路由到 ecommerce-monitor"
```

---

### Task 6: 验证

- [ ] **Step 1: 启动 ecommerce-monitor**

```bash
cd ecommerce-monitor
mvn spring-boot:run
```

Expected: 日志显示 `http://localhost:8094/admin`，Nacos 注册成功

- [ ] **Step 2: 启动 ecommerce-auth**

```bash
cd ecommerce-auth
mvn spring-boot:run
```

Expected: 服务启动后，SBA Wallboard 自动出现 ecommerce-auth

- [ ] **Step 3: 访问 SBA UI**

打开 `http://localhost:8094/admin`

Expected:
- Wallboard 显示 ecommerce-monitor 和 ecommerce-auth
- 点击 ecommerce-auth → Details → 可看到 Health / Memory / Threads
- DataSource 指标可见（Druid 连接池）

- [ ] **Step 4: 验证 actuator 端点**

```bash
curl http://localhost:8091/actuator/health
```

Expected: `{"status":"UP"}`

---

### 文件清单总览

| 操作 | 文件 | 说明 |
|------|------|------|
| Create | `ecommerce-monitor/pom.xml` | SBA Server 模块 |
| Create | `ecommerce-monitor/src/main/java/.../MonitorApplication.java` | 启动类 + @EnableAdminServer |
| Create | `ecommerce-monitor/src/main/resources/application.yml` | 端口 8094 + /admin |
| Modify | `pom.xml` | version + dependencyManagement + module |
| Modify | `ecommerce-*/pom.xml` ×14 | 加 actuator + SBA client 依赖 |
| Modify | `ecommerce-*/src/main/resources/application.yml` ×14 | 加 management 配置 |
| Modify | `ecommerce-gateway/.../application.yml` | 加 /admin 路由 |
