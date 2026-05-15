package com.ecommerce.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private Integer sortOrder;
    private LocalDateTime createTime;
}
