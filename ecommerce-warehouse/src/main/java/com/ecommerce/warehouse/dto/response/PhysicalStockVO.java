package com.ecommerce.warehouse.dto.response;

import lombok.Data;

@Data
public class PhysicalStockVO {
    private Long id;
    private Long warehouseId;
    private Long skuId;
    private Long binId;
    private Integer quantity;
    private Integer lockedQty;
    private Integer availableQty;
    private Integer safetyStock;
}
