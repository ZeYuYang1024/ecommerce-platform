package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.outbox.OutboxSummary;
import com.ecommerce.payment.dto.request.OutboxRetryRequest;
import com.ecommerce.payment.dto.response.OutboxMessageVO;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class AdminPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private AdminPaymentController controller;

    @Test
    void listOutboxShouldReturnPagedMessages() {
        Page<OutboxMessageVO> page = new Page<>(1, 10, 1);
        OutboxMessageVO message = new OutboxMessageVO();
        ReflectionTestUtils.setField(message, "id", 2001L);
        ReflectionTestUtils.setField(message, "aggregateId", "ORD-9");
        ReflectionTestUtils.setField(message, "topic", "order-paid");
        ReflectionTestUtils.setField(message, "status", 3);
        ReflectionTestUtils.setField(message, "retryCount", 1);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 5, 28, 14, 0));
        page.setRecords(List.of(message));
        when(paymentService.listOutbox(any(), eq(1), eq(10))).thenReturn(page);

        var result = controller.listOutbox(3, "order-paid", "ORD-9", 1, 10);

        assertThat(result.getData().getRecords()).hasSize(1);
        assertThat(result.getData().getRecords().getFirst().getTopic()).isEqualTo("order-paid");
    }

    @Test
    void getOutboxSummaryShouldDelegateToService() {
        OutboxSummary summary = new OutboxSummary(1, 0, 2, 3);
        when(paymentService.getOutboxSummary(any())).thenReturn(summary);

        var result = controller.getOutboxSummary(3, "order-paid", "ORD-9");

        assertThat(result.getData()).isSameAs(summary);
        verify(paymentService).getOutboxSummary(any());
    }

    @Test
    void retryOutboxMessageShouldDelegateToService() {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setMessageId(2001L);
        when(paymentService.retryOutboxMessage(2001L)).thenReturn(1);

        var result = controller.retryOutbox(request);

        assertThat(result.getData()).isEqualTo(1);
        verify(paymentService).retryOutboxMessage(2001L);
    }

    @Test
    void retryOutboxBatchShouldDelegateToService() {
        OutboxRetryRequest request = new OutboxRetryRequest();
        request.setStatus(3);
        request.setTopic("order-paid");
        request.setAggregateId("ORD-9");
        request.setLimit(20);
        when(paymentService.retryOutboxBatch(any(), eq(20))).thenReturn(2);

        var result = controller.retryOutboxBatch(request);

        assertThat(result.getData()).isEqualTo(2);
        verify(paymentService).retryOutboxBatch(any(), eq(20));
    }
}
