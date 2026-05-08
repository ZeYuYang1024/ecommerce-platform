package com.ecommerce.auth.common;

import com.ecommerce.common.result.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    USER_NOT_FOUND(10001001, "用户不存在"),
    USERNAME_DUPLICATE(10001002, "用户名已存在"),
    PASSWORD_ERROR(10001003, "密码错误"),
    TOKEN_EXPIRED(10001004, "Token 已过期"),
    TOKEN_INVALID(10001005, "Token 无效"),
    USER_FORBIDDEN(10001006, "用户已被禁用"),

    ADMIN_NOT_FOUND(10100001, "管理员不存在"),
    ADMIN_PASSWORD_ERROR(10100002, "管理员密码错误"),
    ADMIN_FORBIDDEN(10100003, "管理员已被禁用"),
    PERMISSION_DENIED(10100004, "没有操作权限"),
    ;

    private final int code;
    private final String message;

    AuthErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
