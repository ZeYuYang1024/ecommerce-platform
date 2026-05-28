package com.ecommerce.common.outbox;

public enum OutboxStatus {
    PENDING(0),
    SENDING(1),
    SENT(2),
    FAILED(3);

    private final int code;

    OutboxStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
