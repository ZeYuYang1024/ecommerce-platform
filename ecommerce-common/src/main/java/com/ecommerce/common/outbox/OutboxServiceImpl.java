package com.ecommerce.common.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.util.SnowflakeUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class OutboxServiceImpl implements OutboxService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 20;
    private static final int MAX_BATCH_RETRY_LIMIT = 100;
    private static final int BASE_RETRY_DELAY_SECONDS = 5;
    private static final int MAX_RETRY_DELAY_SECONDS = 300;
    private static final int MAX_ERROR_LENGTH = 512;

    private final OutboxMapper outboxMapper;
    private final OutboxPayloadSerializer serializer;

    public OutboxServiceImpl(OutboxMapper outboxMapper, OutboxPayloadSerializer serializer) {
        this.outboxMapper = outboxMapper;
        this.serializer = serializer;
    }

    @Override
    public void enqueue(String aggregateType, String aggregateId, String topic, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setId(SnowflakeUtils.nextId());
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setTopic(topic);
        message.setPayloadJson(serializer.toJson(payload));
        message.setStatus(OutboxStatus.PENDING.getCode());
        message.setRetryCount(0);
        message.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        message.setNextRetryAt(LocalDateTime.now());
        outboxMapper.insert(message);
    }

    @Override
    public List<OutboxMessage> loadPendingBatch(int limit) {
        Page<OutboxMessage> page = new Page<>(1, Math.max(limit, 1), false);
        LambdaQueryWrapper<OutboxMessage> wrapper = new LambdaQueryWrapper<OutboxMessage>()
                .in(OutboxMessage::getStatus, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode())
                .le(OutboxMessage::getNextRetryAt, LocalDateTime.now())
                .apply("retry_count < max_retry_count")
                .orderByAsc(OutboxMessage::getId);
        return outboxMapper.selectPage(page, wrapper).getRecords();
    }

    @Override
    public boolean markSending(Long id) {
        OutboxMessage update = new OutboxMessage();
        update.setStatus(OutboxStatus.SENDING.getCode());
        update.setLastError(null);
        return outboxMapper.update(update, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .in(OutboxMessage::getStatus, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode()))
                == 1;
    }

    @Override
    public void markSent(Long id) {
        OutboxMessage update = new OutboxMessage();
        update.setId(id);
        update.setStatus(OutboxStatus.SENT.getCode());
        update.setLastError(null);
        outboxMapper.updateById(update);
    }

    @Override
    public void markFailed(OutboxMessage message, String error) {
        int nextRetryCount = (message.getRetryCount() == null ? 0 : message.getRetryCount()) + 1;
        OutboxMessage update = new OutboxMessage();
        update.setId(message.getId());
        update.setStatus(OutboxStatus.FAILED.getCode());
        update.setRetryCount(nextRetryCount);
        update.setLastError(trimError(error));
        update.setNextRetryAt(LocalDateTime.now().plusSeconds(retryDelaySeconds(nextRetryCount)));
        outboxMapper.updateById(update);
    }

    @Override
    public Page<OutboxMessage> queryMessages(OutboxQuery query, int page, int size) {
        Page<OutboxMessage> request = new Page<>(Math.max(page, 1), Math.max(size, 1));
        return outboxMapper.selectPage(request, buildQueryWrapper(query));
    }

    @Override
    public OutboxSummary summarize(OutboxQuery query) {
        List<OutboxMessage> messages = outboxMapper.selectList(buildQueryWrapper(withoutStatus(query)));
        long pendingCount = countByStatus(messages, OutboxStatus.PENDING);
        long sendingCount = countByStatus(messages, OutboxStatus.SENDING);
        long sentCount = countByStatus(messages, OutboxStatus.SENT);
        long failedCount = countByStatus(messages, OutboxStatus.FAILED);
        long exhaustedCount = messages.stream()
                .filter(this::isExhausted)
                .count();
        LocalDateTime oldestRetryableCreatedAt = messages.stream()
                .filter(this::isRetryable)
                .map(OutboxMessage::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new OutboxSummary(pendingCount, sendingCount, sentCount, failedCount,
                exhaustedCount, oldestRetryableCreatedAt);
    }

    @Override
    public boolean retryMessage(Long id) {
        OutboxMessage update = retryResetUpdate();
        return outboxMapper.update(update, new LambdaUpdateWrapper<OutboxMessage>()
                .eq(OutboxMessage::getId, id)
                .in(OutboxMessage::getStatus, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode())) == 1;
    }

    @Override
    public int retryBatch(OutboxQuery query, int limit) {
        validateRetryBatch(query, limit);
        LambdaUpdateWrapper<OutboxMessage> wrapper = new LambdaUpdateWrapper<OutboxMessage>();
        applyQueryFilters(wrapper, query);
        if (query.getStatus() == null) {
            wrapper.in(OutboxMessage::getStatus, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode());
        }
        wrapper.last("limit " + limit);
        return outboxMapper.update(retryResetUpdate(), wrapper);
    }

    private int retryDelaySeconds(int retryCount) {
        int exponent = Math.max(0, retryCount - 1);
        long delay = (long) BASE_RETRY_DELAY_SECONDS * (1L << Math.min(exponent, 8));
        return (int) Math.min(delay, MAX_RETRY_DELAY_SECONDS);
    }

    private String trimError(String error) {
        if (error == null || error.length() <= MAX_ERROR_LENGTH) {
            return error;
        }
        return error.substring(0, MAX_ERROR_LENGTH);
    }

    private OutboxMessage retryResetUpdate() {
        OutboxMessage update = new OutboxMessage();
        update.setStatus(OutboxStatus.PENDING.getCode());
        update.setLastError(null);
        update.setNextRetryAt(LocalDateTime.now());
        return update;
    }

    private long countByStatus(List<OutboxMessage> messages, OutboxStatus status) {
        return messages.stream()
                .filter(message -> Integer.valueOf(status.getCode()).equals(message.getStatus()))
                .count();
    }

    private boolean isExhausted(OutboxMessage message) {
        return Integer.valueOf(OutboxStatus.FAILED.getCode()).equals(message.getStatus())
                && message.getRetryCount() != null
                && message.getMaxRetryCount() != null
                && message.getRetryCount() >= message.getMaxRetryCount();
    }

    private boolean isRetryable(OutboxMessage message) {
        return Integer.valueOf(OutboxStatus.PENDING.getCode()).equals(message.getStatus())
                || Integer.valueOf(OutboxStatus.FAILED.getCode()).equals(message.getStatus());
    }

    private OutboxQuery withoutStatus(OutboxQuery query) {
        if (query == null) {
            return null;
        }
        return new OutboxQuery(query.getAggregateType(), query.getTopic(), null, query.getAggregateId());
    }

    private void validateRetryBatch(OutboxQuery query, int limit) {
        if (query == null || isUnfiltered(query)) {
            throw new IllegalArgumentException("retry batch requires a filter");
        }
        if (limit < 1 || limit > MAX_BATCH_RETRY_LIMIT) {
            throw new IllegalArgumentException("retry batch limit must be between 1 and " + MAX_BATCH_RETRY_LIMIT);
        }
    }

    private boolean isUnfiltered(OutboxQuery query) {
        return query.getAggregateType() == null
                && query.getAggregateId() == null
                && query.getTopic() == null
                && query.getStatus() == null;
    }

    private LambdaQueryWrapper<OutboxMessage> buildQueryWrapper(OutboxQuery query) {
        LambdaQueryWrapper<OutboxMessage> wrapper = new LambdaQueryWrapper<>();
        applyQueryFilters(wrapper, query);
        return wrapper.orderByDesc(OutboxMessage::getCreatedAt)
                .orderByDesc(OutboxMessage::getId);
    }

    private void applyQueryFilters(LambdaQueryWrapper<OutboxMessage> wrapper, OutboxQuery query) {
        if (query == null) {
            return;
        }
        if (query.getAggregateType() != null) {
            wrapper.eq(OutboxMessage::getAggregateType, query.getAggregateType());
        }
        if (query.getAggregateId() != null) {
            wrapper.eq(OutboxMessage::getAggregateId, query.getAggregateId());
        }
        if (query.getTopic() != null) {
            wrapper.eq(OutboxMessage::getTopic, query.getTopic());
        }
        if (query.getStatus() != null) {
            wrapper.eq(OutboxMessage::getStatus, query.getStatus());
        }
    }

    private void applyQueryFilters(LambdaUpdateWrapper<OutboxMessage> wrapper, OutboxQuery query) {
        if (query == null) {
            return;
        }
        if (query.getAggregateType() != null) {
            wrapper.eq(OutboxMessage::getAggregateType, query.getAggregateType());
        }
        if (query.getAggregateId() != null) {
            wrapper.eq(OutboxMessage::getAggregateId, query.getAggregateId());
        }
        if (query.getTopic() != null) {
            wrapper.eq(OutboxMessage::getTopic, query.getTopic());
        }
        if (query.getStatus() != null) {
            wrapper.eq(OutboxMessage::getStatus, query.getStatus());
        }
    }
}
