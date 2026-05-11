package com.ecommerce.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SpuVO {
    private Long id;
    private String name;
    private Long categoryId;
    private Long brandId;
    private String description;
    private String mainImage;
    private String images;
    private String detail;
    private Integer status;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime createdAt;
}
