package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_check_item")
public class StockCheckItem extends BaseEntity {
    private Long checkId;
    private Long skuId;
    private Long binId;
    private Integer systemQty;
    private Integer actualQty;
    private Integer diffQty;
}
