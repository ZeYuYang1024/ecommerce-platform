package com.ecommerce.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuBatchVO {
    private Long skuId;
    private String skuName;
    private Long spuId;
    private String spuName;
    private BigDecimal price;
}
