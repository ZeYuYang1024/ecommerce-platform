package com.ecommerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shipping_order_item")
public class ShippingOrderItem extends BaseEntity {
    private Long shippingId;
    private Long orderItemId;
    private Long skuId;
    private Integer quantity;
}
