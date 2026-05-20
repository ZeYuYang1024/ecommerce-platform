package com.ecommerce.knowledge.service;

import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {
    ChatResponse chat(ChatRequest request, Long userId, String userType);
    ChatResponse merchantChat(ChatRequest request, Long userId, String userType, Long merchantId);
    SseEmitter stream(ChatRequest request, Long userId, String userType);
}
