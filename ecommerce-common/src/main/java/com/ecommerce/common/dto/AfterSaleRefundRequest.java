package com.ecommerce.common.dto;

import java.math.BigDecimal;

public record AfterSaleRefundRequest(Long afterSaleId, Long orderId, String orderNo, Long userId,
                                     String reason, int refundAttempt, BigDecimal amount, String manualRefundKey) {
    public AfterSaleRefundRequest(Long afterSaleId, Long orderId, String orderNo, Long userId,
                                  String reason, int refundAttempt, BigDecimal amount) {
        this(afterSaleId, orderId, orderNo, userId, reason, refundAttempt, amount, null);
    }

    public boolean manual() {
        return manualRefundKey != null;
    }

    public static final String TOPIC = "after-sale-refund-requested";

    public String outcomeKey() {
        if (manual()) return manualRefundKey;
        return refundAttempt == 0 ? afterSaleId.toString() : afterSaleId + ":" + refundAttempt;
    }
}
