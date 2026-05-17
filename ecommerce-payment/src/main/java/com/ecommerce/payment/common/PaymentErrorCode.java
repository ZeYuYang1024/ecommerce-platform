package com.ecommerce.payment.common;

import com.ecommerce.common.result.ErrorCode;

public enum PaymentErrorCode implements ErrorCode {
    PAYMENT_NOT_FOUND(50010001, "支付记录不存在"),
    PAYMENT_ALREADY_PAID(50010002, "订单已支付"),
    PAYMENT_NOT_PAID(50010003, "订单未支付，无法退款"),
    REFUND_ALREADY_EXISTS(50010004, "退款已存在"),
    REFUND_AMOUNT_INVALID(50010005, "退款金额不合法"),
    RECONCILIATION_NOT_FOUND(50010006, "对账记录不存在");

    private final int code;
    private final String message;

    PaymentErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
