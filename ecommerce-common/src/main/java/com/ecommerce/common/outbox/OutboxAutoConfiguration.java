package com.ecommerce.common.outbox;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// 中文：Outbox 只在显式开启且存在 MyBatis 会话工厂时生效，避免非数据库服务误装配。
// English: Outbox is enabled only when explicitly turned on and MyBatis session infrastructure exists, so non-database services are not misconfigured.
@Configuration
@ConditionalOnClass(name = {
        "org.apache.ibatis.session.SqlSessionFactory",
        "org.mybatis.spring.SqlSessionTemplate"
})
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true")
@MapperScan(basePackageClasses = OutboxMapper.class, annotationClass = Mapper.class)
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxPayloadSerializer outboxPayloadSerializer(JsonMapper jsonMapper) {
        return payload -> {
            try {
                return jsonMapper.writeValueAsString(payload);
            } catch (JacksonException e) {
                throw new IllegalStateException("serialize outbox payload failed", e);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxService outboxService(OutboxMapper outboxMapper, OutboxPayloadSerializer serializer) {
        return new OutboxServiceImpl(outboxMapper, serializer);
    }
}
