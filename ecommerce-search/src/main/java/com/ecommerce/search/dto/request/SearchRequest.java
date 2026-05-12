package com.ecommerce.search.dto.request;

import lombok.Data;

@Data
public class SearchRequest {
    private String keyword;
    private Long categoryId;
    private String sort;
    private Integer page = 1;
    private Integer size = 20;
}
