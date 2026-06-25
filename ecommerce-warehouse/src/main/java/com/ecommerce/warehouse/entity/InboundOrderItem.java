package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inbound_order_item")
public class InboundOrderItem extends BaseEntity {
    private Long inboundId;
    private Long skuId;
    private Integer quantity;
    private Integer receivedQty;
    private Long binId;
}
