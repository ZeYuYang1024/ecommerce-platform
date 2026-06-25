package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("outbound_order")
public class OutboundOrder extends BaseEntity {
    private String outboundNo;
    private Long warehouseId;
    private Integer outboundType;
    private Long shippingId;
    private Integer status;
    private Long merchantId;
    private String remark;
}
