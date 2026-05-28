package com.ecommerce.common.outbox;

public interface OutboxPayloadSerializer {
    String toJson(Object payload);
}
