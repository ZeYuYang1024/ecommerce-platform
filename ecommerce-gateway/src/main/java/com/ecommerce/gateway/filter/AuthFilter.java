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
            // admin 接口需要 admin 角色
            if (isAdminPath && !"admin".equals(claims.get("role"))) {
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
}
