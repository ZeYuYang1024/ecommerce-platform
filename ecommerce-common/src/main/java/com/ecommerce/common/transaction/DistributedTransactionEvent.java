package com.ecommerce.common.transaction;

public enum DistributedTransactionEvent {
    BEGIN,
    COMPLETE,
    FAIL,
    COMPENSATE
}
