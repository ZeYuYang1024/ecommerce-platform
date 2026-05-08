# P0 实施计划 — 核心地基

> 目标：基础设施就绪 + 管理后台能管理商品和用户
> 创建日期：2026-05-08

---

## 文件结构总览（P0 全部产物）

```
ecommerce-platform/
├── pom.xml                          # 父 POM（统一依赖管理）
├── docker-compose.yml               # 中间件 Docker 编排
├── ecommerce-common/                # 公共模块（库，不启动）
├── ecommerce-gateway/               # 网关 :8080
├── ecommerce-auth/                  # 认证服务 :8091
├── ecommerce-file/                  # 文件服务 :8090
├── ecommerce-user/                  # 用户服务 :8081
├── ecommerce-product/               # 商品服务 :8082
├── ecommerce-inventory/             # 库存服务 :8083
├── ecommerce-admin/                 # 管理后台 (Vue 3)
└── docs/
    ├── P0-implementation-plan.md     # 本文件
    └── 2026-05-08-ecommerce-platform-design.md
```

---

## 第一部分：项目脚手架 + 中间件

### Task 1: 创建父 POM

**文件：** `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ecommerce</groupId>
    <artifactId>ecommerce-platform</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>ecommerce-platform</name>

    <modules>
        <module>ecommerce-common</module>
        <module>ecommerce-gateway</module>
        <module>ecommerce-auth</module>
        <module>ecommerce-file</module>
        <module>ecommerce-user</module>
        <module>ecommerce-product</module>
        <module>ecommerce-inventory</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <spring-boot.version>4.0.0</spring-boot.version>
        <spring-cloud.version>2025.0.0</spring-cloud.version>
        <mybatis-plus.version>3.5.16</mybatis-plus.version>
        <hutool.version>5.8.44</hutool.version>
        <jjwt.version>0.12.6</jjwt.version>
        <mysql.version>8.0.33</mysql.version>
        <minio.version>8.5.10</minio.version>
        <nacos.version>2025.0.0</nacos.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-jsqlparser</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- HuTool -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <!-- MySQL -->
            <dependency>
                <groupId>com.mysql</groupId>
                <artifactId>mysql-connector-j</artifactId>
                <version>${mysql.version}</version>
            </dependency>
            <!-- MinIO -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <!-- Nacos Discovery + Config -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
                <version>${nacos.version}</version>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
                <version>${nacos.version}</version>
            </dependency>
            <!-- 公共模块 -->
            <dependency>
                <groupId>com.ecommerce</groupId>
                <artifactId>ecommerce-common</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

---

### Task 2: 创建 docker-compose.yml

**文件：** `docker-compose.yml`

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0
    container_name: ecommerce-mysql
    restart: unless-stopped
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
    volumes:
      - mysql-data:/var/lib/mysql
    command:
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7.2
    container_name: ecommerce-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    command: redis-server --requirepass root
    volumes:
      - redis-data:/data

  nacos:
    image: nacos/nacos-server:v2.4.0
    container_name: ecommerce-nacos
    restart: unless-stopped
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname
    volumes:
      - nacos-data:/home/nacos/data

  rocketmq-namesrv:
    image: apache/rocketmq:5.2.0
    container_name: ecommerce-rmq-namesrv
    restart: unless-stopped
    ports:
      - "9876:9876"
    command: sh mqnamesrv
    volumes:
      - rocketmq-namesrv-data:/home/rocketmq/logs

  rocketmq-broker:
    image: apache/rocketmq:5.2.0
    container_name: ecommerce-rmq-broker
    restart: unless-stopped
    ports:
      - "10911:10911"
      - "10909:10909"
    environment:
      NAMESRV_ADDR: rocketmq-namesrv:9876
    command: sh mqbroker -n rocketmq-namesrv:9876
    depends_on:
      - rocketmq-namesrv
    volumes:
      - rocketmq-broker-data:/home/rocketmq/logs

  minio:
    image: minio/minio:latest
    container_name: ecommerce-minio
    restart: unless-stopped
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: minio
      MINIO_ROOT_PASSWORD: minio
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data

volumes:
  mysql-data:
  redis-data:
  nacos-data:
  rocketmq-namesrv-data:
  rocketmq-broker-data:
  minio-data:
```

验证：

```bash
docker-compose up -d
docker ps  # 6 个容器全部 running
```

---

### Task 3: 创建各模块基础目录

```bash
cd /d/ecommerce-platform

# 后端模块
for module in ecommerce-common ecommerce-gateway ecommerce-auth ecommerce-file ecommerce-user ecommerce-product ecommerce-inventory; do
  mkdir -p "$module/src/main/java/com/ecommerce/${module#ecommerce-}"
  mkdir -p "$module/src/main/resources"
  mkdir -p "$module/src/test/java/com/ecommerce/${module#ecommerce-}"
done
```

每个模块需要最小 `pom.xml` + 启动类 + `application.yml`（后续任务逐个创建）。

---

## 第二部分：ecommerce-common 公共模块

### Task 4: ecommerce-common pom.xml

**文件：** `ecommerce-common/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

---

### Task 5: ErrorCode 接口 + Result + BusinessException

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/result/ErrorCode.java`

```java
package com.ecommerce.common.result;

public interface ErrorCode {
    int getCode();
    String getMessage();
}
```

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/result/Result.java`

```java
package com.ecommerce.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result() {}

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/result/BusinessException.java`

```java
package com.ecommerce.common.result;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

---

### Task 6: 全局异常处理器

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/exception/GlobalExceptionHandler.java`

```java
package com.ecommerce.common.exception;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return Result.fail(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return Result.fail(400, msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
```

---

### Task 7: 工具类

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/util/JwtUtils.java`

```java
package com.ecommerce.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final String SECRET = "ecommerce-platform-jwt-secret-key-256bits!!";
    private static final long EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L; // 7 天

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generate(Long userId, String username, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(getKey())
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public static String getRole(String token) {
        return parse(token).get("role", String.class);
    }
}
```

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/util/SnowflakeUtils.java`

```java
package com.ecommerce.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public class SnowflakeUtils {
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return String.valueOf(nextId());
    }
}
```

**文件：** `ecommerce-common/src/main/java/com/ecommerce/common/config/MybatisPlusConfig.java`

```java
package com.ecommerce.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**通用实体基类：** `ecommerce-common/src/main/java/com/ecommerce/common/entity/BaseEntity.java`

```java
package com.ecommerce.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
```

---

### Task 8: Common 模块 Spring 自动配置（让 GlobalExceptionHandler 生效）

**文件：** `ecommerce-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
com.ecommerce.common.config.MybatisPlusConfig
com.ecommerce.common.exception.GlobalExceptionHandler
```

Common 模块完成。执行：

```bash
cd /d/ecommerce-platform && mvn clean install -pl ecommerce-common
```

---

## 第三部分：ecommerce-gateway 网关

### Task 9: Gateway pom.xml + 启动类 + 配置

**文件：** `ecommerce-gateway/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-gateway</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-gateway/src/main/java/com/ecommerce/gateway/GatewayApplication.java`

```java
package com.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

**文件：** `ecommerce-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: ecommerce-gateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml
    gateway:
      routes:
        - id: auth
          uri: lb://ecommerce-auth
          predicates:
            - Path=/api/v1/auth/**
        - id: file
          uri: lb://ecommerce-file
          predicates:
            - Path=/api/v1/files/**
        - id: user
          uri: lb://ecommerce-user
          predicates:
            - Path=/api/v1/users/**
        - id: product
          uri: lb://ecommerce-product
          predicates:
            - Path=/api/v1/products/**,/api/v1/categories/**,/api/v1/reviews/**
        - id: inventory
          uri: lb://ecommerce-inventory
          predicates:
            - Path=/api/v1/inventory/**
```

---

### Task 10: Gateway JWT 鉴权过滤器

**文件：** `ecommerce-gateway/src/main/java/com/ecommerce/gateway/filter/AuthFilter.java`

```java
package com.ecommerce.gateway.filter;

import com.ecommerce.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITELIST = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/admin/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单放行
        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // 公开 GET 接口放行（商品浏览等）
        if ("GET".equalsIgnoreCase(exchange.getRequest().getMethod().name())
                && !path.contains("/admin")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = JwtUtils.parse(token);
            // 管理员接口需要 admin 角色
            if (path.contains("/admin") && !"admin".equals(claims.get("role"))) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (ExpiredJwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
```

---

## 第四部分：ecommerce-auth 认证服务

### Task 11: Auth Service 基础搭建

**文件：** `ecommerce-auth/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-auth</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/AuthApplication.java`

```java
package com.ecommerce.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

**文件：** `ecommerce-auth/src/main/resources/application.yml`

```yaml
server:
  port: 8091

spring:
  application:
    name: ecommerce-auth
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_auth?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

---

### Task 12: 数据库初始化 SQL

执行以下 SQL（连 MySQL 手动执行，或配置 `spring.sql.init.mode: always`）：

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ecommerce_auth;

CREATE TABLE user (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(256) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(512),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user (
    id BIGINT NOT NULL PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(256) NOT NULL,
    avatar VARCHAR(512),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permission (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(128) NOT NULL,
    type VARCHAR(20) COMMENT 'menu/button/api',
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(256),
    icon VARCHAR(64),
    sort INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin_user_role (
    id BIGINT NOT NULL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (admin_user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permission (
    id BIGINT NOT NULL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### Task 13: Auth Service 实体 + Mapper

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/entity/User.java`

```java
package com.ecommerce.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String phone;
    private String avatar;
    private Integer status;
}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/entity/AdminUser.java`

```java
package com.ecommerce.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_user")
public class AdminUser extends BaseEntity {
    private String username;
    private String password;
    private String avatar;
    private Integer status;
}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/mapper/UserMapper.java`

```java
package com.ecommerce.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/mapper/AdminUserMapper.java`

```java
package com.ecommerce.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {}
```

---

### Task 14: Auth Service 错误码枚举

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/common/AuthErrorCode.java`

```java
package com.ecommerce.auth.common;

import com.ecommerce.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND(1001001, "用户不存在"),
    USERNAME_DUPLICATE(1001002, "用户名已存在"),
    PASSWORD_ERROR(1001003, "密码错误"),
    TOKEN_EXPIRED(1001004, "Token 已过期"),
    TOKEN_INVALID(1001005, "Token 无效"),
    USER_FORBIDDEN(1001006, "用户已被禁用"),

    ADMIN_NOT_FOUND(1010001, "管理员不存在"),
    ADMIN_PASSWORD_ERROR(1010002, "管理员密码错误"),
    ADMIN_FORBIDDEN(1010003, "管理员已被禁用"),
    PERMISSION_DENIED(1010004, "没有操作权限"),
    ;

    private final int code;
    private final String message;
}
```

---

### Task 15: Auth Service DTO

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/dto/request/LoginRequest.java`

```java
package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/dto/request/RegisterRequest.java`

```java
package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为3-32位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度为6-32位")
    private String password;

    private String phone;
}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/dto/response/LoginResponse.java`

```java
package com.ecommerce.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
}
```

---

### Task 16: Auth Service 业务逻辑

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/service/AuthService.java`

```java
package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    LoginResponse adminLogin(LoginRequest request);
    Long validateToken(String token);
}
```

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/service/impl/AuthServiceImpl.java`

```java
package com.ecommerce.auth.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.ecommerce.auth.common.AuthErrorCode;
import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.entity.AdminUser;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.mapper.AdminUserMapper;
import com.ecommerce.auth.mapper.UserMapper;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.common.util.SnowflakeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;

    @Override
    public LoginResponse register(RegisterRequest request) {
        boolean exists = userMapper.exists(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (exists) {
            throw new BusinessException(AuthErrorCode.USERNAME_DUPLICATE);
        }

        User user = new User();
        user.setId(SnowflakeUtils.nextId());
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword()));
        user.setPhone(request.getPhone());
        userMapper.insert(user);

        String token = JwtUtils.generate(user.getId(), user.getUsername(), "user");
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(AuthErrorCode.USER_FORBIDDEN);
        }

        String token = JwtUtils.generate(user.getId(), user.getUsername(), "user");
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public LoginResponse adminLogin(LoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, request.getUsername()));
        if (admin == null) {
            throw new BusinessException(AuthErrorCode.ADMIN_NOT_FOUND);
        }
        if (!BCrypt.checkpw(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(AuthErrorCode.ADMIN_PASSWORD_ERROR);
        }
        if (admin.getStatus() == 0) {
            throw new BusinessException(AuthErrorCode.ADMIN_FORBIDDEN);
        }

        String token = JwtUtils.generate(admin.getId(), admin.getUsername(), "admin");
        return LoginResponse.builder()
                .token(token)
                .userId(admin.getId())
                .username(admin.getUsername())
                .build();
    }

    @Override
    public Long validateToken(String token) {
        try {
            return JwtUtils.getUserId(token);
        } catch (Exception e) {
            throw new BusinessException(AuthErrorCode.TOKEN_INVALID);
        }
    }
}
```

---

### Task 17: Auth Controller

**文件：** `ecommerce-auth/src/main/java/com/ecommerce/auth/controller/AuthController.java`

```java
package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.request.LoginRequest;
import com.ecommerce.auth.dto.request.RegisterRequest;
import com.ecommerce.auth.dto.response.LoginResponse;
import com.ecommerce.auth.service.AuthService;
import com.ecommerce.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.adminLogin(request));
    }

    @GetMapping("/validate")
    public Result<Long> validate(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Result.ok(authService.validateToken(token));
    }
}
```

---

**Auth Service 完成。** 启动验证：

```bash
cd /d/ecommerce-platform && mvn clean package -pl ecommerce-common,ecommerce-auth
java -jar ecommerce-auth/target/ecommerce-auth-1.0-SNAPSHOT.jar

# 测试注册
curl -X POST http://localhost:8091/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 测试登录
curl -X POST http://localhost:8091/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

---

## 第五部分：ecommerce-file 文件服务

### Task 18: File Service 基础搭建

**文件：** `ecommerce-file/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-file</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/FileApplication.java`

```java
package com.ecommerce.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
```

**文件：** `ecommerce-file/src/main/resources/application.yml`

```yaml
server:
  port: 8090

spring:
  application:
    name: ecommerce-file
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

minio:
  endpoint: http://localhost:9000
  access-key: minio
  secret-key: minio
  bucket: ecommerce
```

---

### Task 19: File Service 错误码 + MinIO 配置

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/common/FileErrorCode.java`

```java
package com.ecommerce.file.common;

import com.ecommerce.common.result.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileErrorCode implements ErrorCode {
    FILE_SIZE_EXCEED(2001001, "文件大小超过限制"),
    FILE_TYPE_UNSUPPORTED(2001002, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(2001003, "文件上传失败"),
    FILE_NOT_FOUND(2001004, "文件不存在"),
    ;

    private final int code;
    private final String message;
}
```

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/config/MinioConfig.java`

```java
package com.ecommerce.file.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    // getters/setters...
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
}
```

---

### Task 20: File Service 业务逻辑

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/service/FileService.java`

```java
package com.ecommerce.file.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    String upload(MultipartFile file);
    void delete(String objectName);
    String getUrl(String objectName);
}
```

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/service/impl/FileServiceImpl.java`

```java
package com.ecommerce.file.service.impl;

import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.file.common.FileErrorCode;
import com.ecommerce.file.config.MinioConfig;
import com.ecommerce.file.service.FileService;
import com.ecommerce.common.result.BusinessException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Override
    public String upload(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException(FileErrorCode.FILE_SIZE_EXCEED);
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BusinessException(FileErrorCode.FILE_TYPE_UNSUPPORTED);
        }

        String ext = getExtension(file.getOriginalFilename());
        String objectName = SnowflakeUtils.nextIdStr() + ext;

        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public String getUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
        }
    }

    private void ensureBucket() throws Exception {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(minioConfig.getBucket()).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucket()).build());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
```

---

### Task 21: File Controller

**文件：** `ecommerce-file/src/main/java/com/ecommerce/file/controller/FileController.java`

```java
package com.ecommerce.file.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    @DeleteMapping("/{objectName}")
    public Result<Void> delete(@PathVariable String objectName) {
        fileService.delete(objectName);
        return Result.ok();
    }

    @GetMapping("/{objectName}/url")
    public Result<String> getUrl(@PathVariable String objectName) {
        return Result.ok(fileService.getUrl(objectName));
    }
}
```

---

## 第六部分：ecommerce-user 用户服务

### Task 22: User Service 基础搭建

**文件：** `ecommerce-user/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-user</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-user/src/main/resources/application.yml`

```yaml
server:
  port: 8081

spring:
  application:
    name: ecommerce-user
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_user?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**文件：** `ecommerce-user/src/main/java/com/ecommerce/user/UserApplication.java`

```java
package com.ecommerce.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
```

数据库 ecommerce_user 的先建表：

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_user CHARACTER SET utf8mb4;

USE ecommerce_user;

CREATE TABLE address (
    id BIGINT NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    province VARCHAR(64),
    city VARCHAR(64),
    district VARCHAR(64),
    detail VARCHAR(256) NOT NULL,
    is_default TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

User Service 实体 + Mapper + DTO + Service + Controller 参照 Auth Service 的模式搭建，接口清单：

| 接口 | 说明 |
|---|---|
| `GET /api/v1/users/me` | 获取当前用户信息 |
| `PUT /api/v1/users/me` | 更新用户信息（昵称、头像） |
| `GET /api/v1/users/addresses` | 地址列表 |
| `POST /api/v1/users/addresses` | 新增地址 |
| `PUT /api/v1/users/addresses/{id}` | 更新地址 |
| `DELETE /api/v1/users/addresses/{id}` | 删除地址 |
| `PUT /api/v1/users/addresses/{id}/default` | 设为默认地址 |

User Service 错误码前缀 `10xx`。

---

## 第七部分：ecommerce-product 商品服务

### Task 23: Product Service — 基础搭建 + 数据库 + 实体

**文件：** `ecommerce-product/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-product</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-product/src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: ecommerce-product
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_product?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**文件：** `ecommerce-product/src/main/java/com/ecommerce/product/ProductApplication.java`

```java
package com.ecommerce.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
```

数据库 ecommerce_product：

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_product CHARACTER SET utf8mb4;

USE ecommerce_product;

CREATE TABLE category (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    level INT DEFAULT 1,
    sort INT DEFAULT 0,
    icon VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE brand (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    logo VARCHAR(512),
    description VARCHAR(256),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE spu (
    id BIGINT NOT NULL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    description TEXT,
    main_image VARCHAR(512),
    images TEXT COMMENT 'JSON array of image URLs',
    detail TEXT COMMENT '富文本详情',
    status TINYINT DEFAULT 0 COMMENT '0=下架 1=上架',
    avg_rating DECIMAL(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sku (
    id BIGINT NOT NULL PRIMARY KEY,
    spu_id BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    spec JSON COMMENT '规格JSON, e.g. {"颜色":"黑色","内存":"128GB"}',
    price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2),
    image VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_spu_id (spu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review (
    id BIGINT NOT NULL PRIMARY KEY,
    spu_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(64),
    order_id BIGINT,
    rating TINYINT NOT NULL COMMENT '1-5星',
    content VARCHAR(1024),
    images TEXT COMMENT 'JSON array',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_spu_id (spu_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

Product Service 实体类：`Category`、`Brand`、`Spu`、`Sku`、`Review`（继承 BaseEntity，参照 Auth Service 写法）。

---

### Task 24: Product Service 接口清单

错误码前缀 `20xx`。

| 接口 | 说明 |
|---|---|
| `GET /api/v1/categories` | 分类树 |
| `POST /api/v1/admin/categories` | 新增分类 |
| `PUT /api/v1/admin/categories/{id}` | 更新分类 |
| `DELETE /api/v1/admin/categories/{id}` | 删除分类 |
| `GET /api/v1/products` | 分页商品列表（支持分类/状态筛选） |
| `GET /api/v1/products/{id}` | 商品详情（含 SKU 列表 + 评论列表） |
| `POST /api/v1/admin/products` | 新增商品（含 SKU） |
| `PUT /api/v1/admin/products/{id}` | 更新商品 |
| `PUT /api/v1/admin/products/{id}/status` | 上下架 |
| `GET /api/v1/products/{spuId}/reviews` | 商品评论列表 |
| `POST /api/v1/products/{spuId}/reviews` | 发表评论 |
| `GET /api/v1/admin/products` | 管理端商品列表 |

---

## 第八部分：ecommerce-inventory 库存服务

### Task 25: Inventory Service — 基础搭建 + 数据库 + 接口

**文件：** `ecommerce-inventory/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.ecommerce</groupId>
        <artifactId>ecommerce-platform</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>

    <artifactId>ecommerce-inventory</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot4-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ecommerce</groupId>
            <artifactId>ecommerce-common</artifactId>
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

**文件：** `ecommerce-inventory/src/main/resources/application.yml`

```yaml
server:
  port: 8083

spring:
  application:
    name: ecommerce-inventory
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_inventory?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
        file-extension: yaml
        shared-configs:
          - data-id: common.yaml
            group: DEFAULT_GROUP
            refresh: true
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml

mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**文件：** `ecommerce-inventory/src/main/java/com/ecommerce/inventory/InventoryApplication.java`

```java
package com.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
```

数据库 ecommerce_inventory：

```sql
CREATE DATABASE IF NOT EXISTS ecommerce_inventory CHARACTER SET utf8mb4;

USE ecommerce_inventory;

CREATE TABLE stock (
    id BIGINT NOT NULL PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    total_stock INT DEFAULT 0 COMMENT '总库存',
    locked_stock INT DEFAULT 0 COMMENT '已锁定库存（下单未支付）',
    available_stock INT DEFAULT 0 COMMENT '可用库存 = total - locked',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

错误码前缀 `30xx`。

| 接口 | 说明 |
|---|---|
| `GET /api/v1/inventory/{skuId}` | 查询 SKU 库存 |
| `POST /api/v1/inventory/batch-query` | 批量查询库存 |
| `POST /api/v1/inventory/deduct` | 扣减库存（下单） |
| `POST /api/v1/inventory/release` | 释放库存（取消订单） |
| `POST /api/v1/admin/inventory/{skuId}` | 设置库存（管理端） |

**扣减库存 Service 核心逻辑（带乐观锁）：**

```java
@Transactional
public void deduct(Long skuId, int quantity) {
    Stock stock = stockMapper.selectOne(
            new LambdaQueryWrapper<Stock>().eq(Stock::getSkuId, skuId));
    if (stock == null || stock.getAvailableStock() < quantity) {
        throw new BusinessException(InventoryErrorCode.STOCK_INSUFFICIENT);
    }

    int updated = stockMapper.update(null,
            new LambdaUpdateWrapper<Stock>()
                    .eq(Stock::getSkuId, skuId)
                    .eq(Stock::getVersion, stock.getVersion())
                    .setSql("locked_stock = locked_stock + " + quantity)
                    .setSql("available_stock = available_stock - " + quantity)
                    .setSql("version = version + 1"));

    if (updated == 0) {
        throw new BusinessException(InventoryErrorCode.STOCK_UPDATE_FAILED);
    }
}
```

---

## 第九部分：ecommerce-admin 管理后台

### Task 26: Vue 3 项目创建 + 基础配置

```bash
cd /d/ecommerce-platform
npm create vite@latest ecommerce-admin -- --template vue
cd ecommerce-admin
npm install
npm install element-plus @element-plus/icons-vue axios pinia vue-router
npm install -D sass
```

**文件：** `ecommerce-admin/vite.config.js`

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
```

**文件：** `ecommerce-admin/src/main.js`

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.mount('#app')
```

---

### Task 27: Admin 路由 + Layout

**文件：** `ecommerce-admin/src/router/index.js`

```js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { noAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '数据概览' } },
      { path: 'products', name: 'Products', component: () => import('@/views/product/list.vue'), meta: { title: '商品管理' } },
      { path: 'products/create', name: 'ProductCreate', component: () => import('@/views/product/form.vue'), meta: { title: '新增商品' } },
      { path: 'products/:id/edit', name: 'ProductEdit', component: () => import('@/views/product/form.vue'), meta: { title: '编辑商品' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/category/index.vue'), meta: { title: '分类管理' } },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/inventory/index.vue'), meta: { title: '库存管理' } },
      { path: 'users', name: 'Users', component: () => import('@/views/user/list.vue'), meta: { title: '用户管理' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.noAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

---

### Task 28: Admin 核心页面

**Layout：** `ecommerce-admin/src/layout/index.vue`

```vue
<template>
  <el-container style="height:100vh">
    <el-aside width="220px" style="background:#001529">
      <div style="color:white;text-align:center;padding:20px;font-size:20px;font-weight:bold">
        电商管理后台
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#ffffff80"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>数据概览</span>
        </el-menu-item>
        <el-sub-menu index="goods">
          <template #title><el-icon><Goods /></el-icon><span>商品管理</span></template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/products/create">新增商品</el-menu-item>
          <el-menu-item index="/categories">分类管理</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/inventory">
          <el-icon><Box /></el-icon><span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #eee">
        <span>{{ $route.meta.title }}</span>
        <el-button @click="logout">退出登录</el-button>
      </el-header>
      <el-main><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>
```

**登录页：** `ecommerce-admin/src/views/login/index.vue`

```vue
<template>
  <div style="display:flex;justify-content:center;align-items:center;height:100vh;background:#f5f5f5">
    <el-card style="width:400px">
      <template #header><h2 style="text-align:center">管理后台登录</h2></template>
      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width:100%" @click="login" :loading="loading">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function login() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const { data } = await axios.post('/api/v1/auth/admin/login', form)
    if (data.code === 200) {
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('username', data.data.username)
      router.push('/dashboard')
    } else {
      ElMessage.error(data.message)
    }
  } finally {
    loading.value = false
  }
}
</script>
```

**商品列表页核心代码：** `ecommerce-admin/src/views/product/list.vue`

```vue
<template>
  <div>
    <div style="margin-bottom:16px;display:flex;gap:12px">
      <el-input v-model="keyword" placeholder="搜索商品名称" style="width:240px" clearable @clear="fetchData" />
      <el-select v-model="status" placeholder="状态" style="width:120px" clearable @change="fetchData">
        <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
      </el-select>
      <el-button type="primary" @click="$router.push('/products/create')">新增商品</el-button>
    </div>
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="120" />
      <el-table-column prop="name" label="商品名称" />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const status = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/products', {
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined, status: status.value }
    })
    if (data.code === 200) {
      tableData.value = data.data.records
      total.value = data.data.total
    }
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/v1/admin/products/${row.id}/status`, { status: newStatus })
  row.status = newStatus
}

async function handleDelete(id) {
  await axios.delete(`/api/v1/admin/products/${id}`)
  fetchData()
}

onMounted(fetchData)
</script>
```

---

## P0 验证清单

所有模块完成后依次验证：

```
[ ] docker-compose up -d → 6 个容器 running
[ ] 启动 Nacos → localhost:8848 可访问
[ ] 启动 Auth → POST /api/v1/auth/register + /login 正常
[ ] 启动 File → POST /api/v1/files/upload 上传图片成功
[ ] 启动 User → GET /api/v1/users/me 返回用户信息
[ ] 启动 Product → POST /api/v1/admin/products 创建商品成功
[ ] 启动 Inventory → GET /api/v1/inventory/{skuId} 返回库存
[ ] 启动 Gateway → localhost:8080 所有路由正常转发
[ ] 启动 Admin → localhost:5173 登录 → 商品管理 CRUD
```

---

## 执行顺序

```
Task 1 → Task 3     (项目骨架)
Task 2               (Docker)
Task 4 → Task 8     (Common 模块)
Task 9 → Task 10    (Gateway)
Task 11 → Task 17   (Auth)
Task 18 → Task 21   (File)
Task 22              (User)
Task 23 → Task 24   (Product)
Task 25              (Inventory)
Task 26 → Task 28   (Admin 前端)
```
