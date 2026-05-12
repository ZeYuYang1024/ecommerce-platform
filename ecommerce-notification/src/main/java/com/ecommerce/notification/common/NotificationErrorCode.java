package com.ecommerce.notification.common;

import com.ecommerce.common.result.ErrorCode;

public enum NotificationErrorCode implements ErrorCode {
    TEMPLATE_NOT_FOUND(80010001, "通知模板不存在"),
    SEND_FAILED(80010002, "通知发送失败"),
    CHANNEL_NOT_SUPPORTED(80010003, "不支持的通知渠道"),
    ;

    private final int code;
    private final String message;

    NotificationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override public int getCode() { return code; }
    @Override public String getMessage() { return message; }
}
