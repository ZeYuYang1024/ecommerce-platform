package com.ecommerce.knowledge.dto.request;

import lombok.Data;

@Data
public class CreateDocumentRequest {
    private Long categoryId;
    private String title;
    private String content;
    private String sourceType;
}
