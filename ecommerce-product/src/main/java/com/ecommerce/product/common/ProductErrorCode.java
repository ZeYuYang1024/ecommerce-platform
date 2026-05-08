package com.ecommerce.product.common;

import com.ecommerce.common.result.ErrorCode;

public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(20010001, "商品不存在"),
    CATEGORY_NOT_FOUND(20010002, "分类不存在"),
    SKU_NOT_FOUND(20010003, "SKU 不存在"),
    ;

    private final int code;
    private final String message;

    ProductErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
