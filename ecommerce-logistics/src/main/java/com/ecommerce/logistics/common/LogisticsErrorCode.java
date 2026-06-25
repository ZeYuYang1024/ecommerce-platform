package com.ecommerce.logistics.common;

import com.ecommerce.common.result.ErrorCode;

public enum LogisticsErrorCode implements ErrorCode {
    PROVIDER_NOT_FOUND(50020001, "provider not found"),
    PROVIDER_CODE_EXISTS(50020002, "provider code already exists"),
    SHIPPING_NOT_FOUND(50020003, "shipping order not found"),
    SHIPPING_DUPLICATE(50020004, "duplicate shipping request"),
    ORDER_NOT_FOUND(50020005, "order not found"),
    ORDER_NOT_PAID(50020006, "order is not paid"),
    TRACKING_NOT_FOUND(50020007, "tracking not found"),
    TRACKING_SUBSCRIBE_FAILED(50020008, "tracking subscribe failed"),
    INVALID_STATUS_TRANSITION(50020009, "invalid status transition"),
    CALLBACK_SIGNATURE_INVALID(50020010, "invalid callback signature"),
    QUANTITY_EXCEEDS_ORDER(50020011, "shipping quantity exceeds order quantity"),
    SHIPPING_FORBIDDEN(50020012, "forbidden shipping access"),
    WAREHOUSE_OUTBOUND_FAILED(50020013, "warehouse outbound creation failed"),
    INSUFFICIENT_MANAGED_STOCK(50020014, "managed warehouse stock insufficient"),
    TEMPLATE_NOT_FOUND(50020015, "shipping template not found"),
    TEMPLATE_CALC_FAILED(50020016, "shipping fee calculation failed"),
    WAYBILL_GENERATE_FAILED(50020017, "waybill generation failed"),
    TEMPLATE_FORBIDDEN(50020018, "forbidden template access"),
    WAREHOUSE_FORBIDDEN(50020019, "forbidden warehouse access");

    private final int code;
    private final String message;

    LogisticsErrorCode(int code, String message) {
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
