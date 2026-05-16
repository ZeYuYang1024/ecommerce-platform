package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long skuId;
    private Long spuId;
    private String name;
    private String image;
    private Integer quantity;
    private BigDecimal price;
    private Boolean checked;
}
