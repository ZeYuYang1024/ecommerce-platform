package com.ecommerce.common.dto;

public record CompensationRefundRequest(String orderNo, String reason, boolean replay) {
    public CompensationRefundRequest(String orderNo, String reason) {
        this(orderNo, reason, false);
    }

    public static final String TOPIC = "order-refund-required";
}
