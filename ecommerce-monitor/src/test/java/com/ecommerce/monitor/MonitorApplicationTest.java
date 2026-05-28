package com.ecommerce.monitor;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorApplicationTest {

    @Test
    void monitorApplicationShouldExcludeDatabaseAutoConfiguration() {
        SpringBootApplication annotation = MonitorApplication.class.getAnnotation(SpringBootApplication.class);

        assertThat(annotation).isNotNull();
        assertThat(Arrays.asList(annotation.exclude()))
                .contains(DataSourceAutoConfiguration.class, MybatisPlusAutoConfiguration.class);
    }
}
