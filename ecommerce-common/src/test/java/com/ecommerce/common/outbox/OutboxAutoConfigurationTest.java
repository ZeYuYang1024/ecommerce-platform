package com.ecommerce.common.outbox;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxAutoConfigurationTest {

    @Test
    void contextShouldNotRegisterOutboxBeansWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .withBean(JsonMapper.class, JsonMapper::new)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OutboxPayloadSerializer.class);
                    assertThat(context).doesNotHaveBean(OutboxService.class);
                    assertThat(context).doesNotHaveBean(OutboxMapper.class);
                });
    }

    @Test
    void contextShouldStartWithoutTreatingSerializerAsMapper() {
        SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setEnvironment(new Environment(
                "test",
                new JdbcTransactionFactory(),
                mock(DataSource.class)));
        when(sqlSessionFactory.getConfiguration()).thenReturn(configuration);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .withPropertyValues("outbox.enabled=true")
                .withBean(JsonMapper.class, JsonMapper::new)
                .withBean(SqlSessionFactory.class, () -> sqlSessionFactory)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OutboxPayloadSerializer.class);
                    assertThat(context.getBean("outboxPayloadSerializer")).isInstanceOf(OutboxPayloadSerializer.class);
                    assertThat(context).hasSingleBean(OutboxMapper.class);
                });
    }
}
