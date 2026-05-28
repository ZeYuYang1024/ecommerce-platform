package com.ecommerce.common.outbox;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class OutboxSummary {
    private long pendingCount;
    private long sendingCount;
    private long sentCount;
    private long failedCount;
    private long exhaustedCount;
    private LocalDateTime oldestRetryableCreatedAt;

    public OutboxSummary(long pendingCount, long sendingCount, long sentCount, long failedCount) {
        this(pendingCount, sendingCount, sentCount, failedCount, 0, null);
    }

    public OutboxSummary(long pendingCount, long sendingCount, long sentCount, long failedCount,
                         long exhaustedCount, LocalDateTime oldestRetryableCreatedAt) {
        this.pendingCount = pendingCount;
        this.sendingCount = sendingCount;
        this.sentCount = sentCount;
        this.failedCount = failedCount;
        this.exhaustedCount = exhaustedCount;
        this.oldestRetryableCreatedAt = oldestRetryableCreatedAt;
    }
}
