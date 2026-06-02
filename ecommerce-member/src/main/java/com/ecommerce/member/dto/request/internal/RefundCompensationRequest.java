package com.ecommerce.member.dto.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompensationRequest {
    private String refundNo;
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundType;
    private String idempotencyKey;
}
