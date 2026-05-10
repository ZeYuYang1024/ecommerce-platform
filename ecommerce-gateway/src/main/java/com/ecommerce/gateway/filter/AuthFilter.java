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
            "/api/v1/files"
    );

    // 商家不可访问的路径
    private static final List<String> MERCHANT_FORBIDDEN = Arrays.asList(
            "/api/v1/admin/merchants",
            "/api/v1/admin/users",
            "/api/v1/admin/dashboard",
            "/api/v1/admin/reconciliation",
            "/api/v1/admin/settlements",
            "/api/v1/admin/brands",
            "/api/v1/admin/categories",
            "/api/v1/admin/reviews"
    );

    // 运营不可访问的路径（可以看 dashboard，但不能审核商家/管用户）
    private static final List<String> OPS_FORBIDDEN = Arrays.asList(
            "/api/v1/admin/merchants",
            "/api/v1/admin/users"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // 白名单：登录注册直接放行
        if (AUTH_WHITELIST.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        // admin 路径必须鉴权，不受公开 GET 影响
        boolean isAdminPath = path.startsWith("/api/v1/admin");

        // 公开 GET：商品浏览、文件访问（admin 路径除外）
        if (!isAdminPath && "GET".equalsIgnoreCase(method)) {
            for (String prefix : PUBLIC_GET_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return chain.filter(exchange);
                }
            }
        }

        // 其余所有请求需要登录
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = JwtUtils.parse(token);
            // 注入 header 供下游服务使用
            Long userId = Long.valueOf(claims.getSubject());
            String userType = claims.get("type", String.class);
            final Long merchantId;
            Object mid = claims.get("merchantId");
            if (mid instanceof Number n) merchantId = n.longValue();
            else merchantId = null;
            exchange = exchange.mutate()
                    .request(r -> {
                        r.header("X-User-Id", String.valueOf(userId));
                        r.header("X-User-Type", userType != null ? userType : "user");
                        if (merchantId != null) r.header("X-Merchant-Id", String.valueOf(merchantId));
                    })
                    .build();
            // admin 接口需要 admin 角色
            if (isAdminPath && !"admin".equals(claims.get("role"))) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            // 多角色权限控制
            if (isAdminPath && userType != null) {
                if ("merchant".equals(userType)) {
                    for (String forbidden : MERCHANT_FORBIDDEN) {
                        if (path.startsWith(forbidden)) {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }
                    }
                } else if ("ops".equals(userType)) {
                    for (String forbidden : OPS_FORBIDDEN) {
                        if (path.startsWith(forbidden)) {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }
                    }
                }
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
}
