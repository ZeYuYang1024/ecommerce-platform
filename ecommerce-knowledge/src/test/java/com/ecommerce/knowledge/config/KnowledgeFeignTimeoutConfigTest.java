package com.ecommerce.knowledge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeFeignTimeoutConfigTest {

    @Test
    void applicationYamlShouldDefineFeignTimeoutsForKnowledgeClients() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application", new ClassPathResource("application.yml"));

        assertThat(sources).isNotEmpty();
        PropertySource<?> source = sources.getFirst();
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.connectTimeout")).isEqualTo(200);
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.readTimeout")).isEqualTo(500);
    }
}
