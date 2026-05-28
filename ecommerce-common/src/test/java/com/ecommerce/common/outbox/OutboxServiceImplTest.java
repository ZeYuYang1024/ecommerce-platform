package com.ecommerce.common.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.OrderInventoryMessage;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {

    @BeforeAll
    static void initMybatisMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new Configuration(), "outbox-test");
        assistant.setCurrentNamespace("outbox-test");
        TableInfoHelper.initTableInfo(assistant, OutboxMessage.class);
    }

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private OutboxPayloadSerializer serializer;

    @Test
    void enqueueShouldPersistPendingOutboxRow() {
        when(serializer.toJson(any())).thenReturn("{\"orderNo\":\"ORD-1\"}");

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        service.enqueue("order", "ORD-1", "order-created", new OrderInventoryMessage("ORD-1", List.of()));

        verify(outboxMapper).insert(argThat((OutboxMessage message) ->
                "order".equals(message.getAggregateType())
                        && "ORD-1".equals(message.getAggregateId())
                        && "order-created".equals(message.getTopic())
                        && "{\"orderNo\":\"ORD-1\"}".equals(message.getPayloadJson())
                        && Integer.valueOf(OutboxStatus.PENDING.getCode()).equals(message.getStatus())
                        && Integer.valueOf(0).equals(message.getRetryCount())));
    }

    @Test
    void markFailedShouldIncrementRetryAndScheduleRetry() {
        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        OutboxMessage message = new OutboxMessage();
        message.setId(1L);
        message.setRetryCount(1);

        service.markFailed(message, "mq down");

        verify(outboxMapper).updateById(argThat((OutboxMessage updated) ->
                Long.valueOf(1L).equals(updated.getId())
                        && Integer.valueOf(2).equals(updated.getRetryCount())
                        && Integer.valueOf(OutboxStatus.FAILED.getCode()).equals(updated.getStatus())
                        && updated.getLastError() != null
                        && updated.getLastError().contains("mq down")
                        && updated.getNextRetryAt() != null));
    }

    @Test
    void queryMessagesShouldFilterByStatusAndTopic() {
        LambdaQueryWrapper<?>[] wrapperHolder = new LambdaQueryWrapper<?>[1];
        Page<?>[] pageHolder = new Page<?>[1];
        when(outboxMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            pageHolder[0] = invocation.getArgument(0);
            wrapperHolder[0] = invocation.getArgument(1);
            return pageWith(message(1L, "order", "ORD-1", "order-created", OutboxStatus.FAILED.getCode()));
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        Page<OutboxMessage> result = service.queryMessages(
                new OutboxQuery("order", "order-created", OutboxStatus.FAILED.getCode(), "ORD-1"), 1, 10);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(pageHolder[0].getCurrent()).isEqualTo(1L);
        assertThat(pageHolder[0].getSize()).isEqualTo(10L);
        assertThat(wrapperHolder[0].getSqlSegment()).contains("aggregateType", "topic", "status", "aggregateId");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values())
                .contains("order", "order-created", OutboxStatus.FAILED.getCode(), "ORD-1");
    }

    @Test
    void summarizeShouldCountMessagesByStatusAndIgnoreStatusFilter() {
        LambdaQueryWrapper<?>[] wrapperHolder = new LambdaQueryWrapper<?>[1];
        when(outboxMapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            wrapperHolder[0] = invocation.getArgument(0);
            return List.of(
                    message(1L, "order", "ORD-1", "order-created", OutboxStatus.PENDING.getCode(), 0, 20),
                    message(2L, "order", "ORD-2", "order-created", OutboxStatus.SENDING.getCode(), 0, 20),
                    message(3L, "order", "ORD-3", "order-created", OutboxStatus.SENT.getCode(), 0, 20),
                    message(4L, "order", "ORD-4", "order-created", OutboxStatus.FAILED.getCode(), 20, 20),
                    message(5L, "order", "ORD-5", "order-created", OutboxStatus.FAILED.getCode(), 1, 20));
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        OutboxSummary result = service.summarize(
                new OutboxQuery("order", "order-created", OutboxStatus.FAILED.getCode(), null));

        assertThat(result.getPendingCount()).isEqualTo(1);
        assertThat(result.getSendingCount()).isEqualTo(1);
        assertThat(result.getSentCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(2);
        assertThat(result.getExhaustedCount()).isEqualTo(1);
        assertThat(result.getOldestRetryableCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 28, 12, 1));
        assertThat(wrapperHolder[0].getSqlSegment()).contains("aggregateType", "topic").doesNotContain("status");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values()).contains("order", "order-created");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values()).doesNotContain(OutboxStatus.FAILED.getCode());
    }

    @Test
    void retryMessageShouldResetFailedMessageToPending() {
        OutboxMessage[] updateHolder = new OutboxMessage[1];
        LambdaUpdateWrapper<?>[] wrapperHolder = new LambdaUpdateWrapper<?>[1];
        when(outboxMapper.update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            updateHolder[0] = invocation.getArgument(0);
            wrapperHolder[0] = invocation.getArgument(1);
            return 1;
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        boolean retried = service.retryMessage(1001L);

        assertThat(retried).isTrue();
        assertThat(updateHolder[0].getStatus()).isEqualTo(OutboxStatus.PENDING.getCode());
        assertThat(updateHolder[0].getRetryCount()).isNull();
        assertThat(updateHolder[0].getLastError()).isNull();
        assertThat(updateHolder[0].getNextRetryAt()).isNotNull();
        assertThat(wrapperHolder[0].getSqlSegment()).contains("id", "status", "IN");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values())
                .contains(1001L, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode());
    }

    @Test
    void retryMessageShouldRejectSendingAndSentRows() {
        LambdaUpdateWrapper<?>[] wrapperHolder = new LambdaUpdateWrapper<?>[1];
        when(outboxMapper.update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            wrapperHolder[0] = invocation.getArgument(1);
            return 0;
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        boolean retried = service.retryMessage(1002L);

        assertThat(retried).isFalse();
        assertThat(wrapperHolder[0].getSqlSegment()).contains("id", "status", "IN");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values())
                .contains(1002L, OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode());
    }

    @Test
    void retryBatchShouldReturnAffectedCount() {
        OutboxMessage[] updateHolder = new OutboxMessage[1];
        LambdaUpdateWrapper<?>[] wrapperHolder = new LambdaUpdateWrapper<?>[1];
        when(outboxMapper.update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            updateHolder[0] = invocation.getArgument(0);
            wrapperHolder[0] = invocation.getArgument(1);
            return 3;
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        int affected = service.retryBatch(new OutboxQuery("payment", null, OutboxStatus.FAILED.getCode(), null), 50);

        assertThat(affected).isEqualTo(3);
        assertThat(updateHolder[0].getStatus()).isEqualTo(OutboxStatus.PENDING.getCode());
        assertThat(updateHolder[0].getRetryCount()).isNull();
        assertThat(updateHolder[0].getLastError()).isNull();
        assertThat(updateHolder[0].getNextRetryAt()).isNotNull();
        assertThat(wrapperHolder[0].getSqlSegment()).contains("status", "limit 50");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values())
                .contains("payment", OutboxStatus.FAILED.getCode());
    }

    @Test
    void retryBatchShouldStillRestrictToPendingAndFailedWhenStatusFilterIsMissing() {
        LambdaUpdateWrapper<?>[] wrapperHolder = new LambdaUpdateWrapper<?>[1];
        when(outboxMapper.update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            wrapperHolder[0] = invocation.getArgument(1);
            return 2;
        });

        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);
        int affected = service.retryBatch(new OutboxQuery("payment", "order-paid", null, null), 20);

        assertThat(affected).isEqualTo(2);
        assertThat(wrapperHolder[0]).isNotNull();
        assertThat(wrapperHolder[0].getSqlSegment()).contains("status", "IN");
        assertThat(wrapperHolder[0].getParamNameValuePairs().values())
                .contains("payment", "order-paid", OutboxStatus.PENDING.getCode(), OutboxStatus.FAILED.getCode());
    }

    @Test
    void retryBatchShouldRejectMissingFilter() {
        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);

        assertThatThrownBy(() -> service.retryBatch(null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filter");

        verify(outboxMapper, never()).update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void retryBatchShouldRejectUnfilteredQuery() {
        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);

        assertThatThrownBy(() -> service.retryBatch(new OutboxQuery(null, null, null, null), 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filter");

        verify(outboxMapper, never()).update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    void retryBatchShouldRejectLimitAboveMaximum() {
        OutboxServiceImpl service = new OutboxServiceImpl(outboxMapper, serializer);

        assertThatThrownBy(() -> service.retryBatch(
                new OutboxQuery("payment", null, OutboxStatus.FAILED.getCode(), null), 1000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");

        verify(outboxMapper, never()).update(any(OutboxMessage.class), any(LambdaUpdateWrapper.class));
    }

    private Page<OutboxMessage> pageWith(OutboxMessage message) {
        Page<OutboxMessage> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(message));
        return page;
    }

    private OutboxMessage message(Long id, String aggregateType, String aggregateId, String topic, int status) {
        return message(id, aggregateType, aggregateId, topic, status, 0, 20);
    }

    private OutboxMessage message(Long id, String aggregateType, String aggregateId, String topic, int status,
                                  int retryCount, int maxRetryCount) {
        OutboxMessage message = new OutboxMessage();
        message.setId(id);
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setTopic(topic);
        message.setStatus(status);
        message.setRetryCount(retryCount);
        message.setMaxRetryCount(maxRetryCount);
        message.setNextRetryAt(LocalDateTime.now());
        message.setCreatedAt(LocalDateTime.of(2026, 5, 28, 12, 0).plusMinutes(id));
        return message;
    }
}
