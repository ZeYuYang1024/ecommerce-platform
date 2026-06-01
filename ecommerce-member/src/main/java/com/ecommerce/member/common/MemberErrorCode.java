package com.ecommerce.member.common;

import com.ecommerce.common.result.ErrorCode;

public enum MemberErrorCode implements ErrorCode {
    MEMBER_PROFILE_NOT_FOUND(10020001, "会员档案不存在"),
    INSUFFICIENT_POINTS(10020002, "积分不足"),
    ALREADY_CHECKED_IN(10020003, "今日已签到"),
    LEVEL_NOT_FOUND(10020004, "会员等级不存在"),
    INVALID_POINTS_AMOUNT(10020005, "积分数量无效"),
    DUPLICATE_BIZ_KEY(10020006, "重复的业务操作"),
    CONCURRENT_UPDATE_FAILED(10020007, "并发更新失败，请重试"),
    ORDER_NOT_FOUND(10020008, "订单不存在");

    private final int code;
    private final String message;

    MemberErrorCode(int code, String message) {
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
