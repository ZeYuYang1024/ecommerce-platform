package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import com.ecommerce.knowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/v1/knowledge/chat")
    public Result<ChatResponse> chat(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                     @RequestHeader(value = "X-User-Type", required = false) String userType,
                                     @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request, userId, userType));
    }

    @PostMapping("/api/v1/admin/knowledge/chat")
    public Result<ChatResponse> adminChat(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                          @RequestHeader(value = "X-User-Type", required = false) String userType,
                                          @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request, userId, userType));
    }

    @PostMapping("/api/v1/admin/merchant/knowledge/chat")
    public Result<ChatResponse> merchantChat(@RequestHeader(value = "X-User-Id", required = false) Long userId,
                                             @RequestHeader(value = "X-User-Type", required = false) String userType,
                                             @RequestHeader("X-Merchant-Id") Long merchantId,
                                             @RequestBody ChatRequest request) {
        return Result.ok(chatService.merchantChat(request, userId, userType, merchantId));
    }
}
