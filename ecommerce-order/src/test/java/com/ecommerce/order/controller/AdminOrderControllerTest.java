package com.ecommerce.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.order.dto.request.OutboxRetryRequest;
import com.ecommerce.order.dto.response.OutboxMessageVO;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController controller;

    @Test
    void listOutboxShouldReturnPagedMessages() {
        Page<OutboxMessageVO> page = new Page<>(1, 10, 1);
        OutboxMessageVO message = new OutboxMessageVO();
        ReflectionTestUtils.setField(message, "id", 1001L);
        ReflectionTestUtils.setField(message, "aggregateId", "ORD-1");
        ReflectionTestUtils.setField(message, "topic", "order-created");
        ReflectionTestUtils.setField(message, "status", 3);
        ReflectionTestUtils.setField(message, "retryCount", 2);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 5, 28, 12, 0));
        page.setRecords(List.of(message));
        when(orderService.listOutbox(any(), eq(1), eq(10))).thenReturn(page);

        var result = controller.listOutbox(3, "order-created", "ORD-1", 1, 10);

        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getRecords().getFirst().getTopic()).isEqualTo("order-created");
    }

    @Test
    void retryOutboxMessageShouldDelegateToService() {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setMessageId(1001L);
        when(orderService.retryOutboxMessage(1001L)).thenReturn(1);

        var result = controller.retryOutbox(request);

        assertThat(result.getData()).isEqualTo(1);
        verify(orderService).retryOutboxMessage(1001L);
    }

    @Test
    void retryOutboxBatchShouldDelegateToService() {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setStatus(3);
        request.setTopic("order-created");
        request.setAggregateId("ORD-2");
        request.setLimit(20);
        when(orderService.retryOutboxBatch(any(), eq(20))).thenReturn(2);

        var result = controller.retryOutboxBatch(request);

        assertThat(result.getData()).isEqualTo(2);
        verify(orderService).retryOutboxBatch(any(), eq(20));
    }

    @Test
    void getOutboxSummaryShouldDelegateToService() {
        OutboxSummary summary = new OutboxSummary(1, 0, 2, 3);
        when(orderService.getOutboxSummary(any())).thenReturn(summary);

        var result = controller.getOutboxSummary(3, "order-created", "ORD-3");

        assertThat(result.getData()).isSameAs(summary);
        verify(orderService).getOutboxSummary(any());
    }
}
