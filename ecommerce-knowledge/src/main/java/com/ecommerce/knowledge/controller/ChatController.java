package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.common.Result;
import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.dto.response.ChatResponse;
import com.ecommerce.knowledge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/v1/knowledge/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request));
    }

    @PostMapping("/api/v1/admin/knowledge/chat")
    public Result<ChatResponse> adminChat(@RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request));
    }
}
