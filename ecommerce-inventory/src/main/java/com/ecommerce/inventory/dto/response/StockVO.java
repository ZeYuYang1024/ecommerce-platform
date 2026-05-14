package com.ecommerce.inventory.dto.response;

import lombok.Data;

@Data
public class StockVO {
    private Long id;
    private Long skuId;
    private String skuName;
    private String spuName;
    private java.math.BigDecimal price;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
}
