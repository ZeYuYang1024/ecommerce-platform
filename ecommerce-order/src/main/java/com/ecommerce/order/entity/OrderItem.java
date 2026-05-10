package com.ecommerce.order.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_item")
public class OrderItem extends BaseEntity {
    private Long orderId;
    private String orderNo;
    private Long skuId;
    private Long spuId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
}
