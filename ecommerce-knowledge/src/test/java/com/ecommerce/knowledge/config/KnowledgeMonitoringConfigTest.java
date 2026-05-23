package com.ecommerce.knowledge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeMonitoringConfigTest {

    @Test
    void applicationYamlShouldExposeHealthDetailsAndRegisterAdminClient() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application", new ClassPathResource("application.yml"));

        assertThat(sources).isNotEmpty();
        PropertySource<?> source = sources.getFirst();
        assertThat(source.getProperty("management.endpoints.web.exposure.include")).isEqualTo("*");
        assertThat(source.getProperty("management.endpoint.health.show-details")).isEqualTo("ALWAYS");
        assertThat(source.getProperty("spring.boot.admin.client.url")).isEqualTo("http://localhost:8094/admin");
        assertThat(source.getProperty("spring.data.redis.host")).isEqualTo("${REDIS_HOST:localhost}");
        assertThat(source.getProperty("spring.data.redis.port")).isEqualTo("${REDIS_PORT:6379}");
        assertThat(source.getProperty("spring.data.redis.password")).isEqualTo("${REDIS_PASSWORD:root}");
    }

    @Test
    void pomShouldIncludeSpringBootAdminClientDependency() throws Exception {
        String pom = Files.readString(Path.of("D:/ecommerce-platform/ecommerce-knowledge/pom.xml"), StandardCharsets.UTF_8);

        assertThat(pom).contains("<artifactId>spring-boot-admin-starter-client</artifactId>");
        assertThat(pom).contains("<artifactId>spring-boot-starter-data-redis</artifactId>");
        assertThat(pom).doesNotContain("<artifactId>micrometer-registry-prometheus</artifactId>");
    }
}
