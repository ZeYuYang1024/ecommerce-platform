package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse_bin")
public class WarehouseBin extends BaseEntity {
    private Long zoneId;
    private Long warehouseId;
    private String binCode;
    private Integer binType;
}
