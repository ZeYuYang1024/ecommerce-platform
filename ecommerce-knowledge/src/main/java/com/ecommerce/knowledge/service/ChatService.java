package com.ecommerce.knowledge.service;

import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;

public interface ChatService {
    ChatResponse chat(ChatRequest request);
}
