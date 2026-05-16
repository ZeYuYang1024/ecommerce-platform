package com.ecommerce.product.common;

import com.ecommerce.common.result.ErrorCode;

public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(20010001, "商品不存在"),
    CATEGORY_NOT_FOUND(20010002, "分类不存在"),
    SKU_NOT_FOUND(20010003, "SKU 不存在"),
    INVALID_PRICE_FORMAT(20010004, "价格格式不正确"),
    PRODUCT_FORBIDDEN(20010005, "无权操作该商品"),
    BRAND_NOT_FOUND(20010006, "品牌不存在"),
    BRAND_FORBIDDEN(20010007, "无权操作该品牌"),
    BRAND_AUDIT_STATUS_INVALID(20010008, "品牌审核状态不合法");

    private final int code;
    private final String message;

    ProductErrorCode(int code, String message) {
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
