package com.ecommerce.knowledge.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "code is required")
    private String code;

    private Long parentId;
    private Integer sortOrder;
}
