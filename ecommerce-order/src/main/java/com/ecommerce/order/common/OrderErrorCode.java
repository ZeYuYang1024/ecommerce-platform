package com.ecommerce.order.common;

import com.ecommerce.common.result.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(40010001, "订单不存在"),
    ORDER_NOT_PENDING(40010002, "订单不在待支付状态"),
    ORDER_ALREADY_CANCELLED(40010003, "订单已取消"),
    ORDER_ITEMS_EMPTY(40010004, "订单商品不能为空"),
    ORDER_NOT_PAID(40010005, "订单未支付"),
    ORDER_ALREADY_SHIPPED(40010006, "订单已发货"),
    ;

    private final int code;
    private final String message;

    OrderErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
