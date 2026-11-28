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
    MANUAL_REFUND_INVALID(40010023, "当前订单状态或剩余可退金额不支持此次退款"),
    MANUAL_REFUND_IN_PROGRESS(40010024, "该订单已有待处理的退款，请先处理"),
    SHIPMENT_RECONCILIATION_REQUIRED(40010025, "仓库已出库但缺少物流单，请人工核对，当前占用已保留"),
    AFTER_SALE_NOT_FAILED(40010015, "仅退款失败的申请可以重试"),
    ORDER_ITEM_UNAVAILABLE(40010016, "商品已下架或不可购买"),
    ORDER_ITEM_INVALID_QUANTITY(40010017, "商品数量须大于零"),
    ORDER_INVENTORY_UNCONFIRMED(40010018, "库存尚未确认，请稍后发货"),
    ORDER_NO_COMPENSATION_REFUND(40010019, "当前订单没有待处理的补偿退款"),
    CHECKOUT_CONFLICT(40010020, "这次结算已绑定其他商品或地址"),
    CHECKOUT_EXPIRED(40010021, "订单已超过支付时限");

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
