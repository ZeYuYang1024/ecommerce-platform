package com.ecommerce.order.common;

import com.ecommerce.common.result.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(40010001, "订单不存在"),
    ORDER_NOT_PENDING(40010002, "订单不在待支付状态"),
    ORDER_ALREADY_CANCELLED(40010003, "订单已取消"),
    ORDER_ITEMS_EMPTY(40010004, "订单商品不能为空"),
    ORDER_NOT_PAID(40010005, "订单未支付"),
    ORDER_ALREADY_SHIPPED(40010006, "订单已发货"),
    ORDER_FORBIDDEN(40010007, "无权操作该订单"),
    SHIPPING_MUST_USE_LOGISTICS(40010008, "请使用物流服务创建发货单"),
    AFTER_SALE_NOT_FOUND(40010009, "售后申请不存在"),
    AFTER_SALE_NOT_ELIGIBLE(40010010, "仅支持已支付且未开始发货的订单申请全额退款"),
    AFTER_SALE_ALREADY_REVIEWED(40010011, "售后申请已处理，请刷新"),
    AFTER_SALE_REVIEW_NOTE_REQUIRED(40010012, "请填写拒绝原因"),
    AFTER_SALE_BLOCKS_SHIPPING(40010013, "该订单有待处理的退款申请，请先处理售后"),
    AFTER_SALE_CATALOG_UNAVAILABLE(40010014, "暂时无法确认店铺，请稍后重新申请"),
    AFTER_SALE_NOT_FAILED(40010015, "仅退款失败的申请可以重试");

    private final int code;
    private final String message;

    OrderErrorCode(int code, String message) {
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
