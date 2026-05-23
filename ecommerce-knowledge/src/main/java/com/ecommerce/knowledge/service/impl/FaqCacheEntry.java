package com.ecommerce.knowledge.service.impl;

import com.ecommerce.knowledge.dto.response.ChatResponse;

import java.util.List;

record FaqCacheEntry(String answer, List<ChatResponse.Source> sources, long cachedAtMillis) {

    FaqCacheEntry(String answer, List<ChatResponse.Source> sources) {
        this(answer, sources, System.currentTimeMillis());
    }
}
