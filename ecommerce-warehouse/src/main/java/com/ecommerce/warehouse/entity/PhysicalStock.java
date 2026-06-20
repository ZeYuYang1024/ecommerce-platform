package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("physical_stock")
public class PhysicalStock extends BaseEntity {
    private Long warehouseId;
    private Long skuId;
    private Long binId;
    private Integer quantity;
    private Integer lockedQty;
    private Integer availableQty;
    private Integer safetyStock;
}
