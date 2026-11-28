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
    private BigDecimal cumulativeRefundAmount;

    public RefundCompensationRequest(String refundNo, String orderNo, Long userId, BigDecimal refundAmount,
                                     String refundType, String idempotencyKey) {
        this(refundNo, orderNo, userId, refundAmount, refundType, idempotencyKey, null);
    }

    public BigDecimal cumulativeAmount() {
        return cumulativeRefundAmount == null ? refundAmount : cumulativeRefundAmount;
    }
}
