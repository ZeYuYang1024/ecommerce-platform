package com.ecommerce.payment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund")
public class Refund extends BaseEntity {
    private String refundNo;
    private Long paymentId;
    private String orderNo;
    private BigDecimal amount;
    private String reason;
    private Integer status;
}
