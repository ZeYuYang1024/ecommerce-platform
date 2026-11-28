package com.ecommerce.common.dto;

import java.math.BigDecimal;

public record AfterSaleRefundRequest(Long afterSaleId, Long orderId, String orderNo, Long userId,
                                     String reason, int refundAttempt, BigDecimal amount) {
    public static final String TOPIC = "after-sale-refund-requested";

    public String outcomeKey() {
        return refundAttempt == 0 ? afterSaleId.toString() : afterSaleId + ":" + refundAttempt;
    }
}
