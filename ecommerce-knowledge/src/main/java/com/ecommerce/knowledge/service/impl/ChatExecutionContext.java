package com.ecommerce.knowledge.service.impl;

import com.ecommerce.knowledge.chat.KnowledgeLightRoute;
import com.ecommerce.knowledge.chat.KnowledgeQueryFeatures;
import com.ecommerce.knowledge.chat.KnowledgeQueryRoute;

record ChatExecutionContext(
        String sessionId,
        KnowledgeQueryFeatures features,
        KnowledgeLightRoute lightRoute,
        KnowledgeQueryRoute route) {
}
