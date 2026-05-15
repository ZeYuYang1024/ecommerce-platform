package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private String brandName;
    private String categoryName;
    private Integer totalStock;
}
