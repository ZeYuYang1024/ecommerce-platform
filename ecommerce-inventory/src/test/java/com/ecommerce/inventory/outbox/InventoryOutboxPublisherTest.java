package com.ecommerce.inventory.outbox;

import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryOutboxPublisherTest {

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
        OrderPaidMessage payload = new OrderPaidMessage("ORD-1", 4, LocalDateTime.of(2026, 5, 28, 10, 0));
        when(outboxService.loadPendingBatch(20)).thenReturn(List.of(message));
        when(outboxService.markSending(1L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class)).thenReturn(payload);

        InventoryOutboxPublisher publisher = new InventoryOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper, 20);
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
        OrderPaidMessage payload = new OrderPaidMessage("ORD-2", 4, LocalDateTime.of(2026, 5, 28, 11, 0));
        when(outboxService.loadPendingBatch(20)).thenReturn(List.of(message));
        when(outboxService.markSending(2L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class)).thenReturn(payload);
        doThrow(new RuntimeException("mq down")).when(rocketMQTemplate).syncSend(eq("order-paid"), any(OrderPaidMessage.class));

        InventoryOutboxPublisher publisher = new InventoryOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper, 20);
        publisher.publishPendingMessages();

        verify(outboxService).markFailed(message, "mq down");
        verify(outboxService, never()).markSent(2L);
    }
}
