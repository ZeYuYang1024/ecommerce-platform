package com.ecommerce.knowledge.chat;

public record KnowledgeQueryFeatures(
        String normalizedQuestion,
        boolean userScoped,
        boolean hasOrderNo,
        boolean policyFaq,
        boolean realtimeIntent,
        boolean productIntent,
        boolean inventoryIntent) {
}
