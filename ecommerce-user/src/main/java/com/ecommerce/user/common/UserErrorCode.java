package com.ecommerce.user.common;

import com.ecommerce.common.result.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(10010001, "用户不存在"),
    ADDRESS_NOT_FOUND(10010002, "地址不存在"),
    ADDRESS_LIMIT_EXCEEDED(10010003, "地址数量超过限制"),
    ;

    private final int code;
    private final String message;

    UserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
