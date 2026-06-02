package com.ecommerce.order.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`order`")
public class Order extends BaseEntity {
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private String pointsReservationNo;
    private Integer pointsUsed;
    private BigDecimal pointsDeductionAmount;
    private Integer pointsDeductionRatio;
    private Integer status;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
}
