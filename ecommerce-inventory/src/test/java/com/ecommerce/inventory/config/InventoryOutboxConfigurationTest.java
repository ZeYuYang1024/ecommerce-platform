package com.ecommerce.inventory.config;

import com.ecommerce.common.outbox.OutboxAutoConfiguration;
import com.ecommerce.common.outbox.OutboxService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryOutboxConfigurationTest {

    @Test
    void applicationConfigShouldEnableOutboxBeans() {
        SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setEnvironment(new Environment(
                "test",
                new JdbcTransactionFactory(),
                mock(DataSource.class)));
        when(sqlSessionFactory.getConfiguration()).thenReturn(configuration);

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration.class))
                .withInitializer(this::loadInventoryApplicationYaml)
                .withBean(JsonMapper.class, JsonMapper::new)
                .withBean(SqlSessionFactory.class, () -> sqlSessionFactory)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(OutboxService.class);
                });
    }

    private void loadInventoryApplicationYaml(ConfigurableApplicationContext context) {
        List<PropertySource<?>> propertySources;
        try {
            propertySources = new YamlPropertySourceLoader().load(
                    "inventory-application",
                    new ClassPathResource("application.yml"));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load inventory application.yml", ex);
        }
        propertySources.forEach(source -> context.getEnvironment().getPropertySources().addLast(source));
    }
}
