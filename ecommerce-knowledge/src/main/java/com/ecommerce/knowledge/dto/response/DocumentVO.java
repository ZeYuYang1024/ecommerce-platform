package com.ecommerce.knowledge.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private String sourceType;
    private String status;
    private Integer chunkCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
