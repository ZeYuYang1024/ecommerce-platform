package com.ecommerce.file.common;

import com.ecommerce.common.result.ErrorCode;

public enum FileErrorCode implements ErrorCode {
    FILE_SIZE_EXCEED(2001001, "文件大小超过限制"),
    FILE_TYPE_UNSUPPORTED(2001002, "不支持的文件类型"),
    FILE_UPLOAD_FAILED(2001003, "文件上传失败"),
    FILE_NOT_FOUND(2001004, "文件不存在"),
    ;

    private final int code;
    private final String message;

    FileErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
