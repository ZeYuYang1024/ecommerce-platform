package com.ecommerce.merchant.common;

import com.ecommerce.common.result.ErrorCode;

public enum MerchantErrorCode implements ErrorCode {
    MERCHANT_NOT_FOUND(60010001, "商家不存在"),
    MERCHANT_NAME_EXISTS(60010002, "店铺名称已存在"),
    MERCHANT_ALREADY_APPROVED(60010003, "商家已通过审核"),
    MERCHANT_NOT_PENDING(60010004, "商家不在待审核状态"),
    INVALID_AUDIT_ACTION(60010005, "无效的审核操作"),
    ;

    private final int code;
    private final String message;

    MerchantErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
