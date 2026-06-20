package com.ecommerce.warehouse.common;

import com.ecommerce.common.result.ErrorCode;

public enum WarehouseErrorCode implements ErrorCode {
    WAREHOUSE_NOT_FOUND(50030001, "仓库不存在"),
    WAREHOUSE_CODE_EXISTS(50030002, "仓库编码已存在"),
    ZONE_NOT_FOUND(50030003, "货区不存在"),
    BIN_NOT_FOUND(50030004, "货位不存在"),
    INBOUND_NOT_FOUND(50030005, "入库单不存在"),
    OUTBOUND_NOT_FOUND(50030006, "出库单不存在"),
    STOCK_NOT_FOUND(50030007, "库存记录不存在"),
    INSUFFICIENT_STOCK(50030008, "库存不足"),
    CHECK_NOT_FOUND(50030009, "盘点单不存在"),
    INVALID_STATUS_TRANSITION(50030010, "非法的状态变更"),
    STOCK_CONCURRENT_UPDATE(50030011, "库存并发更新冲突"),
    WAREHOUSE_DISABLED(50030012, "仓库已停用"),
    NOT_MANAGED_WAREHOUSE(50030013, "非托管库存仓"),
    STOCK_LOCK_FAILED(50030014, "库存锁定失败"),
    DUPLICATE_INBOUND_NO(50030015, "入库单号重复"),
    ZONE_CODE_EXISTS(50030016, "货区编码已存在"),
    BIN_CODE_EXISTS(50030017, "货位编码已存在");

    private final int code;
    private final String message;

    WarehouseErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
