package com.ecommerce.search.common;

import com.ecommerce.common.result.ErrorCode;

public enum SearchErrorCode implements ErrorCode {
    INDEX_ERROR(90010001, "索引操作失败"),
    SEARCH_ERROR(90010002, "搜索异常"),
    ;

    private final int code;
    private final String message;

    SearchErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
