package com.ecommerce.knowledge.chat;

import org.springframework.stereotype.Component;

@Component
public class KnowledgeLightRouteDecider {

    public KnowledgeLightRoute decide(KnowledgeQueryFeatures features, Long userId) {
        if (features == null) {
            return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
        }
        if (features.policyFaq()) {
            return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
        }
        if (userId != null && features.realtimeIntent()) {
            return KnowledgeLightRoute.FAST_PATH_CHANNEL;
        }
        if (features.productIntent() || features.inventoryIntent()) {
            return KnowledgeLightRoute.TOOL_ONLY_AGENT_CHANNEL;
        }
        return KnowledgeLightRoute.RAG_FAQ_CHANNEL;
    }
}
