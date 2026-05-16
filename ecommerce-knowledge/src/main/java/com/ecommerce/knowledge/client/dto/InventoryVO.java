package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryVO {
    private Long id;
    private Long skuId;
    private String skuName;
    private String spuName;
    private BigDecimal price;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
}
