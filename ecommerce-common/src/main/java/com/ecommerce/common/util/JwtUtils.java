package com.ecommerce.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    private static final String SECRET = "ecommerce-platform-jwt-secret-key-256bits!!";
    private static final long EXPIRE_MS = 7 * 24 * 60 * 60 * 1000L;

    private static SecretKey getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generate(Long userId, String username, String role) {
        return generate(userId, username, role, null);
    }

    public static String generate(Long userId, String username, String role, String type) {
        return generate(userId, username, role, type, null);
    }

    public static String generate(Long userId, String username, String role, String type, Long merchantId) {
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", type != null ? type : role);
        if (merchantId != null) builder.claim("merchantId", merchantId);
        return builder.issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(getKey())
                .compact();
    }

    public static String getType(String token) {
        return parse(token).get("type", String.class);
    }

    public static Long getMerchantId(String token) {
        Object val = parse(token).get("merchantId");
        if (val instanceof Number n) return n.longValue();
        return null;
    }

    public static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public static String getRole(String token) {
        return parse(token).get("role", String.class);
    }
}
