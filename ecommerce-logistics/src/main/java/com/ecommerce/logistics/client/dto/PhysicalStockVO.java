package com.ecommerce.logistics.client.dto;

import lombok.Data;

@Data
public class PhysicalStockVO {
    private Long warehouseId;
    private Long skuId;
    private Integer quantity;
    private Integer lockedQty;
    private Integer availableQty;
}
