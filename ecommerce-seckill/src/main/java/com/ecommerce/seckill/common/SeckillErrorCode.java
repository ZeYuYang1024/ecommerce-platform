package com.ecommerce.seckill.common;

import com.ecommerce.common.result.ErrorCode;

public enum SeckillErrorCode implements ErrorCode {
    SESSION_NOT_FOUND(95010001, "秒杀场次不存在"),
    ITEM_NOT_FOUND(95010002, "秒杀商品不存在"),
    STOCK_INSUFFICIENT(95010003, "秒杀库存不足"),
    SESSION_NOT_ACTIVE(95010004, "秒杀场次未开始或已结束"),
    ITEM_DISABLED(95010005, "秒杀商品已禁用"),
    SECKILL_FORBIDDEN(95010006, "无权访问该秒杀活动");

    private final int code;
    private final String message;

    SeckillErrorCode(int code, String message) {
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
