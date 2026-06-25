package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outbound_order_item")
public class OutboundOrderItem extends BaseEntity {
    private Long outboundId;
    private Long skuId;
    private Integer quantity;
    private Integer pickedQty;
    private Integer shippedQty;
    private Long binId;
}
