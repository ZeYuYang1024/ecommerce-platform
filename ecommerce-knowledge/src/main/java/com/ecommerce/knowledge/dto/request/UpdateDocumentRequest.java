package com.ecommerce.knowledge.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Pattern;

@Data
public class UpdateDocumentRequest {
    private Long categoryId;
    private String title;
    private String content;

    @Pattern(regexp = "draft|published|archived", message = "status must be draft, published, or archived")
    private String status;
}
