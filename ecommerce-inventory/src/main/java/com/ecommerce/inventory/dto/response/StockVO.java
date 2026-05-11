package com.ecommerce.inventory.dto.response;

import lombok.Data;

@Data
public class StockVO {
    private Long id;
    private Long skuId;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
}
