package com.ecommerce.recommendation;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
class RecommendationRepository {
    private final JdbcClient jdbc;

    RecommendationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    boolean store(String eventId, long userId, long spuId, RecommendationEventType type, String requestId,
                  Long categoryId, Long brandId, BigDecimal price, Instant occurredAt) {
        try {
            jdbc.sql("""
                    INSERT INTO recommendation_event
                        (event_id, user_id, spu_id, event_type, request_id, category_id, brand_id, price, occurred_at)
                    VALUES (:eventId, :userId, :spuId, :type, :requestId, :categoryId, :brandId, :price, :occurredAt)
                    """)
                    .param("eventId", eventId).param("userId", userId).param("spuId", spuId).param("type", type.name())
                    .param("requestId", requestId, Types.VARCHAR).param("categoryId", categoryId, Types.BIGINT)
                    .param("brandId", brandId, Types.BIGINT).param("price", price, Types.DECIMAL)
                    .param("occurredAt", LocalDateTime.ofInstant(occurredAt, ZoneOffset.UTC)).update();
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    List<Behavior> recent(long userId) {
        return jdbc.sql("""
                SELECT spu_id, category_id, brand_id, price, event_type, occurred_at
                FROM recommendation_event
                WHERE user_id = :userId AND occurred_at >= UTC_TIMESTAMP(3) - INTERVAL 30 DAY
                  AND event_type IN ('VIEW', 'CART_ADD', 'PAID')
                ORDER BY occurred_at DESC, id DESC LIMIT 200
                """).param("userId", userId)
                .query((rs, row) -> new Behavior(rs.getLong("spu_id"), rs.getObject("category_id", Long.class),
                        rs.getObject("brand_id", Long.class), rs.getBigDecimal("price"),
                        RecommendationEventType.valueOf(rs.getString("event_type")),
                        rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)))
                .list();
    }

    List<CategoryScore> categories(long userId) {
        return jdbc.sql("""
                SELECT category_id, score_7d, score_30d
                FROM recommendation_user_category
                WHERE user_id = :userId
                ORDER BY score_7d DESC, score_30d DESC, category_id LIMIT 8
                """).param("userId", userId)
                .query((rs, row) -> new CategoryScore(rs.getLong(1), rs.getDouble(2), rs.getDouble(3))).list();
    }

    List<Related> related(List<Long> seeds) {
        if (seeds.isEmpty()) return List.of();
        return jdbc.sql("""
                SELECT related_spu_id, MAX(affinity) AS score
                FROM recommendation_related_product WHERE spu_id IN (:ids)
                GROUP BY related_spu_id ORDER BY score DESC, related_spu_id LIMIT 36
                """).param("ids", seeds)
                .query((rs, row) -> new Related(rs.getLong(1), rs.getDouble(2))).list();
    }

    record Behavior(long spuId, Long categoryId, Long brandId, BigDecimal price, RecommendationEventType type,
                    Instant occurredAt) {}
    record CategoryScore(long categoryId, double score7d, double score30d) {}
    record Related(long id, double affinity) {}
}
