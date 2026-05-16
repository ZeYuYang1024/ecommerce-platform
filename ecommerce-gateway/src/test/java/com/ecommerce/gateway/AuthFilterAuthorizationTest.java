package com.ecommerce.gateway;

import com.ecommerce.common.util.JwtUtils;
import com.ecommerce.gateway.filter.AuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFilterAuthorizationTest {

    private final AuthFilter filter = new AuthFilter();

    @Test
    void merchantAdminShouldBeForbiddenFromKnowledgeAdminRoutes() {
        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = exchange("POST", "/api/v1/admin/knowledge/chat",
                JwtUtils.generate(1L, "merchant", "admin", "merchant", 100L));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chain.invoked).isFalse();
    }

    @Test
    void merchantAdminShouldBeAllowedToReachProductAdminRoutes() {
        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = exchange("GET", "/api/v1/admin/products",
                JwtUtils.generate(1L, "merchant", "admin", "merchant", 100L));

        filter.filter(exchange, chain).block();

        assertThat(chain.invoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void opsAdminShouldBeAllowedToReachKnowledgeAdminRoutes() {
        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = exchange("POST", "/api/v1/admin/knowledge/chat",
                JwtUtils.generate(2L, "ops", "admin", "ops", null));

        filter.filter(exchange, chain).block();

        assertThat(chain.invoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void opsAdminShouldBeForbiddenFromRbacRoutes() {
        RecordingChain chain = new RecordingChain();
        MockServerWebExchange exchange = exchange("PUT", "/api/v1/admin/users/1/roles",
                JwtUtils.generate(2L, "ops", "admin", "ops", null));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(chain.invoked).isFalse();
    }

    private MockServerWebExchange exchange(String method, String path, String token) {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.valueOf(method), path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private static final class RecordingChain implements GatewayFilterChain {
        private boolean invoked;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.invoked = true;
            return Mono.empty();
        }
    }
}
