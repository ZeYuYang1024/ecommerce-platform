package com.ecommerce.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inbound_order")
public class InboundOrder extends BaseEntity {
    private String inboundNo;
    private Long warehouseId;
    private String inboundType;
    private String sourceOrderNo;
    private Integer status;
    private Long merchantId;
    private String remark;
}
