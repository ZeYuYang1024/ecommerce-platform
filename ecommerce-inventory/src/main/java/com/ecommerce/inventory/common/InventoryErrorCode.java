package com.ecommerce.inventory.common;

import com.ecommerce.common.result.ErrorCode;

public enum InventoryErrorCode implements ErrorCode {
    STOCK_NOT_FOUND(30010001, "库存记录不存在"),
    STOCK_INSUFFICIENT(30010002, "库存不足"),
    STOCK_UPDATE_FAILED(30010003, "库存更新失败，请重试"),
    INVALID_QUANTITY(30010004, "数量必须大于 0"),
    LOCKED_STOCK_INSUFFICIENT(30010005, "锁定库存不足"),
    STOCK_FORBIDDEN(30010006, "无权操作该库存"),
    TOTAL_STOCK_BELOW_LOCKED(30010007, "总库存不能低于已锁定库存");

    private final int code;
    private final String message;

    InventoryErrorCode(int code, String message) {
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
