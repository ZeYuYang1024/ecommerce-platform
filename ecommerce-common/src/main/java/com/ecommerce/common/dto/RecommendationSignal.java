package com.ecommerce.common.dto;

import java.util.List;

public record RecommendationSignal(String eventId, Type type, Long userId, List<Long> spuIds, Long skuId,
                                   long occurredAtMillis) {
    public static final String TOPIC = "recommendation-signal";

    public enum Type { CART_ADD, PAID }
}
