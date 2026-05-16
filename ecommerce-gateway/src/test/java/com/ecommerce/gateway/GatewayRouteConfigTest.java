package com.ecommerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayRouteConfigTest {

    @Test
    void applicationYamlShouldIncludeMerchantAdminRoutes() throws IOException {
        String yaml = new String(
                new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertThat(yaml)
                .contains("/api/v1/admin/merchant/products/**")
                .contains("/api/v1/admin/merchant/brands/**")
                .contains("/api/v1/admin/merchant/reviews/**")
                .contains("/api/v1/admin/merchant/payment/**")
                .contains("/api/v1/admin/merchant/reconciliation/**")
                .contains("/api/v1/admin/merchant/settlement/**")
                .contains("/api/v1/admin/merchant/knowledge/**");
    }
}
