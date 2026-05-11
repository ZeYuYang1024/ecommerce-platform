package com.ecommerce.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuVO {
    private Long id;
    private Long spuId;
    private String name;
    private String spec;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
}
