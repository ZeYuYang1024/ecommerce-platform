package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_check")
public class StockCheck extends BaseEntity {
    private String checkNo;
    private Long warehouseId;
    private Integer status;
    private Long merchantId;
}
