package com.ecommerce.payment.outbox;

import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private JsonMapper jsonMapper;

    @Test
    void publishPendingMessagesShouldSendAndMarkSent() throws Exception {
        OutboxMessage message = new OutboxMessage();
        message.setId(1L);
        message.setTopic("order-paid");
        message.setAggregateId("ORD-1");
        message.setPayloadJson("{\"orderNo\":\"ORD-1\"}");
        OrderPaidMessage payload = new OrderPaidMessage("ORD-1", 1, LocalDateTime.of(2026, 5, 28, 10, 0));
        when(outboxService.loadPendingBatch(20)).thenReturn(List.of(message));
        when(outboxService.markSending(1L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class)).thenReturn(payload);

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper);
        publisher.publishPendingMessages();

        verify(rocketMQTemplate).syncSend("order-paid", payload);
        verify(outboxService).markSent(1L);
    }

    @Test
    void publishPendingMessagesShouldMarkFailedWhenSendFails() throws Exception {
        OutboxMessage message = new OutboxMessage();
        message.setId(2L);
        message.setTopic("order-paid");
        message.setAggregateId("ORD-2");
        message.setPayloadJson("{\"orderNo\":\"ORD-2\"}");
        OrderPaidMessage payload = new OrderPaidMessage("ORD-2", 5, LocalDateTime.of(2026, 5, 28, 11, 0));
        when(outboxService.loadPendingBatch(20)).thenReturn(List.of(message));
        when(outboxService.markSending(2L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class)).thenReturn(payload);
        doThrow(new RuntimeException("mq down")).when(rocketMQTemplate).syncSend(eq("order-paid"), any(OrderPaidMessage.class));

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper);
        publisher.publishPendingMessages();

        verify(outboxService).markFailed(message, "mq down");
        verify(outboxService, never()).markSent(2L);
    }

    @Test
    void publishPendingMessagesShouldSendRefundMessageAndMarkSent() throws Exception {
        OutboxMessage message = new OutboxMessage();
        message.setId(3L);
        message.setTopic("order-refunded");
        message.setAggregateId("ORD-3");
        message.setPayloadJson("{\"orderNo\":\"ORD-3\",\"refundNo\":\"REF-3\"}");
        OrderRefundedMessage payload = new OrderRefundedMessage(
                "REF-3", "ORD-3", 1L, new BigDecimal("88.00"), "FULL", "SUCCESS",
                LocalDateTime.of(2026, 6, 2, 13, 0), "payment-refund:ORD-3:REF-3");
        when(outboxService.loadPendingBatch(20)).thenReturn(List.of(message));
        when(outboxService.markSending(3L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderRefundedMessage.class)).thenReturn(payload);

        PaymentOutboxPublisher publisher = new PaymentOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper);
        publisher.publishPendingMessages();

        verify(rocketMQTemplate).syncSend("order-refunded", payload);
        verify(outboxService).markSent(3L);
    }
}
