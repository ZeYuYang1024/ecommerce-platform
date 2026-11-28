package com.ecommerce.common.outbox;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface OutboxService {
    void enqueue(String aggregateType, String aggregateId, String topic, Object payload);

    List<OutboxMessage> loadPendingBatch(int limit);

    boolean markSending(OutboxMessage message);

    void markSent(OutboxMessage message);

    void markFailed(OutboxMessage message, String error);

    Page<OutboxMessage> queryMessages(OutboxQuery query, int page, int size);

    OutboxSummary summarize(OutboxQuery query);

    boolean retryMessage(Long id);

    int retryBatch(OutboxQuery query, int limit);
}
