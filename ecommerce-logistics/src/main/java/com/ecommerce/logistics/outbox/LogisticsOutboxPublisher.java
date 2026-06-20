package com.ecommerce.logistics.outbox;

import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.common.dto.ShippingExceptionMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class LogisticsOutboxPublisher {

    private final OutboxService outboxService;
    private final RocketMQTemplate rocketMQTemplate;
    private final JsonMapper jsonMapper;
    private final int batchSize;

    public LogisticsOutboxPublisher(OutboxService outboxService, RocketMQTemplate rocketMQTemplate,
                                    JsonMapper jsonMapper,
                                    @Value("${outbox.publisher.batch-size:50}") int batchSize) {
        this.outboxService = outboxService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.jsonMapper = jsonMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPending() {
        for (OutboxMessage message : outboxService.loadPendingBatch(batchSize)) {
            if (!outboxService.markSending(message.getId())) continue;
            try {
                Object payload = switch (message.getTopic()) {
                    case "shipping-dispatched" -> jsonMapper.readValue(message.getPayloadJson(), ShippingDispatchedMessage.class);
                    case "order-delivered" -> jsonMapper.readValue(message.getPayloadJson(), OrderDeliveredMessage.class);
                    case "shipping-exception" -> jsonMapper.readValue(message.getPayloadJson(), ShippingExceptionMessage.class);
                    default -> throw new IllegalStateException("Unknown logistics topic: " + message.getTopic());
                };
                rocketMQTemplate.syncSend(message.getTopic(), payload);
                outboxService.markSent(message.getId());
            } catch (RuntimeException e) {
                log.warn("Publish logistics outbox failed, id={}", message.getId(), e);
                outboxService.markFailed(message, e.getMessage());
            }
        }
    }
}
