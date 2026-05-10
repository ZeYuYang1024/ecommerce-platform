package com.ecommerce.payment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment")
public class Payment extends BaseEntity {
    private String paymentNo;
    private String orderNo;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private String payMethod;
    private LocalDateTime paidAt;
}
