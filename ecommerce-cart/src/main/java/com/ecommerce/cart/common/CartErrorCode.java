package com.ecommerce.cart.common;

import com.ecommerce.common.result.ErrorCode;

public enum CartErrorCode implements ErrorCode {
    CART_ITEM_NOT_FOUND(35010001, "购物车商品不存在"),
    INVALID_QUANTITY(35010002, "数量必须大于0"),
    ;

    private final int code;
    private final String message;

    CartErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
