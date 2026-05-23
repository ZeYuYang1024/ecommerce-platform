package com.ecommerce.knowledge.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class Langchain4jConfigTest {

    @AfterEach
    void tearDown() {
        System.clearProperty("langchain4j.http.clientBuilderFactory");
    }

    @Test
    void chatModelBuildsWithAvailableHttpClient() {
        System.clearProperty("langchain4j.http.clientBuilderFactory");
        Langchain4jConfig config = new Langchain4jConfig();
        ReflectionTestUtils.setField(config, "chatApiKey", "test-key");
        ReflectionTestUtils.setField(config, "chatBaseUrl", "https://api.deepseek.com");
        ReflectionTestUtils.setField(config, "chatModelName", "deepseek-chat");
        ReflectionTestUtils.setField(config, "temperature", 0.7d);
        ReflectionTestUtils.setField(config, "maxTokens", 2000);

        assertDoesNotThrow(config::chatModel);
        assertDoesNotThrow(config::streamingChatModel);
    }
}
