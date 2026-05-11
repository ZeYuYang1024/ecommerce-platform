package com.ecommerce.payment.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVO {
    private Long id;
    private String paymentNo;
    private String orderNo;
    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private String statusText;
    private String payMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
