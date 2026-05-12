package com.ecommerce.coupon.common;

import com.ecommerce.common.result.ErrorCode;

public enum CouponErrorCode implements ErrorCode {
    TEMPLATE_NOT_FOUND(70010001, "优惠券模板不存在"),
    COUPON_EXHAUSTED(70010002, "优惠券已领完"),
    USER_LIMIT_REACHED(70010003, "已达到每人限领数量"),
    COUPON_EXPIRED(70010004, "优惠券已过期"),
    COUPON_NOT_AVAILABLE(70010005, "优惠券不可用"),
    COUPON_ALREADY_USED(70010006, "优惠券已使用"),
    MIN_AMOUNT_NOT_MET(70010007, "未达到最低消费金额"),
    ;

    private final int code;
    private final String message;

    CouponErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
