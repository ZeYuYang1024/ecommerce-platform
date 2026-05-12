package com.ecommerce.search.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductDocument {
    private String id;
    private String name;
    private Long categoryId;
    private String categoryName;
    private Long brandId;
    private String brandName;
    private Long merchantId;
    private String description;
    private String mainImage;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer status;
    private BigDecimal avgRating;
    private Integer reviewCount;
    private Integer salesCount;
    private LocalDateTime createdAt;
}
