package com.ecommerce.gateway.filter;

import com.ecommerce.common.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final List<String> AUTH_WHITELIST = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/admin/login",
            "/api/v1/merchants/register"
    );

    private static final List<String> PUBLIC_GET_PREFIXES = Arrays.asList(
            "/api/v1/products",
            "/api/v1/categories",
            "/api/v1/reviews",
            "/api/v1/files",
            "/api/v1/search",
            "/api/v1/coupons"
    );

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "DELETE", "PATCH");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if (AUTH_WHITELIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        boolean isAdminPath = path.startsWith("/api/v1/admin");
        if (!isAdminPath && "GET".equalsIgnoreCase(method)) {
            for (String prefix : PUBLIC_GET_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return chain.filter(exchange);
                }
            }
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = JwtUtils.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String userType = claims.get("type", String.class);
            Object merchantIdClaim = claims.get("merchantId");
            Long merchantId = merchantIdClaim instanceof Number number ? number.longValue() : null;

            exchange = exchange.mutate()
                    .request(request -> {
                        request.header("X-User-Id", String.valueOf(userId));
                        request.header("X-User-Type", userType != null ? userType : "user");
                        if (merchantId != null) {
                            request.header("X-Merchant-Id", String.valueOf(merchantId));
                        }
                    })
                    .build();

            if (isAdminPath && !"admin".equals(claims.get("role"))) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            if (isAdminPath && !isAllowedAdminRoute(userType, method, path)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (ExpiredJwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isAllowedAdminRoute(String userType, String method, String path) {
        if (userType == null || "super_admin".equals(userType)) {
            return true;
        }
        if ("merchant".equals(userType)) {
            return path.startsWith("/api/v1/admin/products")
                    || path.startsWith("/api/v1/admin/merchant/orders")
                    || ("PUT".equalsIgnoreCase(method) && path.matches("^/api/v1/admin/orders/[^/]+/(ship|status)$"));
        }
        if ("ops".equals(userType)) {
            return path.startsWith("/api/v1/admin/dashboard")
                    || path.startsWith("/api/v1/admin/knowledge")
                    || path.startsWith("/api/v1/admin/payment")
                    || path.startsWith("/api/v1/admin/reconciliation")
                    || path.startsWith("/api/v1/admin/settlements")
                    || (path.equals("/api/v1/admin/orders") && !WRITE_METHODS.contains(method.toUpperCase()))
                    || path.startsWith("/api/v1/admin/orders/recon");
        }
        return false;
    }
}
