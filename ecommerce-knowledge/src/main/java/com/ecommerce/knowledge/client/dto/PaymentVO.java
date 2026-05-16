package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentVO {
    private Long id;
    private String orderNo;
    private String paymentNo;
    private Long userId;
    private String payMethod;
    private Integer status;
    private String statusText;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
