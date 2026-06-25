package com.ecommerce.warehouse.common;

import com.ecommerce.common.result.ErrorCode;

public enum WarehouseErrorCode implements ErrorCode {
    WAREHOUSE_NOT_FOUND(50030001, "warehouse not found"),
    WAREHOUSE_CODE_EXISTS(50030002, "warehouse code already exists"),
    ZONE_NOT_FOUND(50030003, "zone not found"),
    BIN_NOT_FOUND(50030004, "bin not found"),
    INBOUND_NOT_FOUND(50030005, "inbound order not found"),
    OUTBOUND_NOT_FOUND(50030006, "outbound order not found"),
    STOCK_NOT_FOUND(50030007, "stock record not found"),
    INSUFFICIENT_STOCK(50030008, "insufficient stock"),
    CHECK_NOT_FOUND(50030009, "stock check not found"),
    INVALID_STATUS_TRANSITION(50030010, "invalid status transition"),
    STOCK_CONCURRENT_UPDATE(50030011, "concurrent stock update conflict"),
    WAREHOUSE_DISABLED(50030012, "warehouse disabled"),
    NOT_MANAGED_WAREHOUSE(50030013, "warehouse is not managed"),
    STOCK_LOCK_FAILED(50030014, "stock lock failed"),
    DUPLICATE_INBOUND_NO(50030015, "duplicate inbound order number"),
    ZONE_CODE_EXISTS(50030016, "zone code already exists"),
    BIN_CODE_EXISTS(50030017, "bin code already exists"),
    WAREHOUSE_FORBIDDEN(50030018, "forbidden warehouse access");

    private final int code;
    private final String message;

    WarehouseErrorCode(int code, String message) {
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
