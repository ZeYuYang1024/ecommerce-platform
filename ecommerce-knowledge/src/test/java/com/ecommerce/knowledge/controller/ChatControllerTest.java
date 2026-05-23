package com.ecommerce.knowledge.controller;

import com.ecommerce.knowledge.dto.request.ChatRequest;
import com.ecommerce.knowledge.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(chatService)).build();
    }

    @Test
    void shouldExposeStreamingChatEndpoint() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(chatService.stream(any(ChatRequest.class), eq(1001L), eq("USER"))).thenReturn(emitter);

        mockMvc.perform(post("/api/v1/knowledge/chat/stream")
                        .header("X-User-Id", "1001")
                        .header("X-User-Type", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"平台退货规则是什么？","sessionId":"stream-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(chatService).stream(any(ChatRequest.class), eq(1001L), eq("USER"));
    }

    @Test
    void shouldExposeAdminStreamingChatEndpoint() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(chatService.stream(any(ChatRequest.class), eq(2001L), eq("ops"))).thenReturn(emitter);

        mockMvc.perform(post("/api/v1/admin/knowledge/chat/stream")
                        .header("X-User-Id", "2001")
                        .header("X-User-Type", "ops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"平台知识库调试","sessionId":"admin-stream-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(chatService).stream(any(ChatRequest.class), eq(2001L), eq("ops"));
    }

    @Test
    void shouldExposeMerchantStreamingChatEndpoint() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(chatService.merchantStream(any(ChatRequest.class), eq(3001L), eq("merchant"), eq(9001L))).thenReturn(emitter);

        mockMvc.perform(post("/api/v1/admin/merchant/knowledge/chat/stream")
                        .header("X-User-Id", "3001")
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "9001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"商家发货规则是什么","sessionId":"merchant-stream-session"}
                                """))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(chatService).merchantStream(any(ChatRequest.class), eq(3001L), eq("merchant"), eq(9001L));
    }
}
