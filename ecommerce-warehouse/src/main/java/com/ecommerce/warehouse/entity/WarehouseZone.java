package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse_zone")
public class WarehouseZone extends BaseEntity {
    private Long warehouseId;
    private String zoneName;
    private String zoneCode;
    private Integer zoneType;
}
