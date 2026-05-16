package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private Long categoryId;
    private Long brandId;
    private Long merchantId;
    private String description;
    private String mainImage;
    private Integer status;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime createdAt;
}
