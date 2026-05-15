package com.ecommerce.knowledge.dto.request;

import lombok.Data;

@Data
public class CreateCategoryRequest {
    private String name;
    private String code;
    private Long parentId;
    private Integer sortOrder;
}
