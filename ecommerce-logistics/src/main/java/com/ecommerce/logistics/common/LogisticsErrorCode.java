package com.ecommerce.logistics.common;

import com.ecommerce.common.result.ErrorCode;

public enum LogisticsErrorCode implements ErrorCode {
    PROVIDER_NOT_FOUND(50020001, "物流公司不存在"),
    PROVIDER_CODE_EXISTS(50020002, "快递公司编码已存在"),
    SHIPPING_NOT_FOUND(50020003, "发货单不存在"),
    SHIPPING_DUPLICATE(50020004, "重复的发货请求"),
    ORDER_NOT_FOUND(50020005, "订单不存在"),
    ORDER_NOT_PAID(50020006, "订单未支付"),
    TRACKING_NOT_FOUND(50020007, "物流轨迹未找到"),
    TRACKING_SUBSCRIBE_FAILED(50020008, "物流轨迹订阅失败"),
    INVALID_STATUS_TRANSITION(50020009, "非法的状态变更"),
    CALLBACK_SIGNATURE_INVALID(50020010, "回调签名无效"),
    QUANTITY_EXCEEDS_ORDER(50020011, "发货数量超过订单数量"),
    SHIPPING_FORBIDDEN(50020012, "无权访问该发货单"),
    WAREHOUSE_OUTBOUND_FAILED(50020013, "仓储出库单创建失败"),
    INSUFFICIENT_MANAGED_STOCK(50020014, "托管仓库存不足"),
    TEMPLATE_NOT_FOUND(50020015, "运费模板不存在"),
    TEMPLATE_CALC_FAILED(50020016, "运费计算失败");

    private final int code;
    private final String message;

    LogisticsErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
