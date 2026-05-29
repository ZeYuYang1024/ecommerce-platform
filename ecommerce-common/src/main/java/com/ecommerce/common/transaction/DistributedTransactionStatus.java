package com.ecommerce.common.transaction;

public enum DistributedTransactionStatus {
    INIT(0, false),
    PROCESSING(1, false),
    SUCCESS(2, true),
    FAILED(3, true),
    COMPENSATED(4, true);

    private final int code;
    private final boolean terminal;

    DistributedTransactionStatus(int code, boolean terminal) {
        this.code = code;
        this.terminal = terminal;
    }

    public int getCode() {
        return code;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
