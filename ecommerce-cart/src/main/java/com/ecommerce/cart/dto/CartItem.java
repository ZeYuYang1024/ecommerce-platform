package com.ecommerce.cart.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CartItem {
    private Long skuId;
    private Long spuId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private Boolean checked;
}
