package com.ecommerce.knowledge.dto.request;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String sessionId;
}
