package com.ecommerce.knowledge.client.dto;

import lombok.Data;

@Data
public class InventoryVO {
    private Long skuId;
    private String skuCode;
    private String skuName;
    private Integer stock;
    private Integer reservedStock;
    private Integer availableStock;
}
