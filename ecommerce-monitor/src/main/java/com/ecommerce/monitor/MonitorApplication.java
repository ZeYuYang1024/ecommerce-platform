package com.ecommerce.monitor;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

// 中文：监控服务不依赖数据库，因此排除 DataSource 和 MyBatis-Plus 自动配置。
// English: The monitor service does not need a database, so we exclude DataSource and MyBatis-Plus auto-configuration.
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class
})
@EnableAdminServer
@EnableFeignClients
public class MonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }
}
