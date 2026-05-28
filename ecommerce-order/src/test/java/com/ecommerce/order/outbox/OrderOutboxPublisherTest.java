package com.ecommerce.order.outbox;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOutboxPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private JsonMapper jsonMapper;

    private OrderOutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new OrderOutboxPublisher(outboxService, rocketMQTemplate, jsonMapper, 50);
    }

    @Test
    void publishPendingShouldSendAndMarkSent() throws Exception {
        OutboxMessage message = new OutboxMessage();
        message.setId(1L);
        message.setTopic("order-created");
        message.setPayloadJson("{\"orderNo\":\"ORD-1\",\"items\":[]}");
        when(outboxService.loadPendingBatch(50)).thenReturn(List.of(message));
        when(outboxService.markSending(1L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderInventoryMessage.class))
                .thenReturn(new OrderInventoryMessage("ORD-1", List.of()));

        publisher.publishPending();

        verify(rocketMQTemplate).syncSend(eq("order-created"), any(OrderInventoryMessage.class));
        verify(outboxService).markSent(1L);
    }

    @Test
    void publishPendingShouldRecordFailure() throws Exception {
        OutboxMessage message = new OutboxMessage();
        message.setId(1L);
        message.setTopic("order-cancelled");
        message.setPayloadJson("{\"orderNo\":\"ORD-1\",\"items\":[]}");
        when(outboxService.loadPendingBatch(50)).thenReturn(List.of(message));
        when(outboxService.markSending(1L)).thenReturn(true);
        when(jsonMapper.readValue(message.getPayloadJson(), OrderInventoryMessage.class))
                .thenReturn(new OrderInventoryMessage("ORD-1", List.of()));
        doThrow(new RuntimeException("mq down")).when(rocketMQTemplate)
                .syncSend(eq("order-cancelled"), any(OrderInventoryMessage.class));

        publisher.publishPending();

        verify(outboxService).markFailed(eq(message), contains("mq down"));
    }
}
