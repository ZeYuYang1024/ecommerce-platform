package com.ecommerce.inventory.outbox;

import com.ecommerce.common.dto.OrderPaidMessage;
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
public class InventoryOutboxPublisher {

    private final OutboxService outboxService;
    private final RocketMQTemplate rocketMQTemplate;
    private final JsonMapper jsonMapper;
    private final int batchSize;

    public InventoryOutboxPublisher(OutboxService outboxService,
                                    RocketMQTemplate rocketMQTemplate,
                                    JsonMapper jsonMapper,
                                    @Value("${outbox.publisher.batch-size:20}") int batchSize) {
        this.outboxService = outboxService;
        this.rocketMQTemplate = rocketMQTemplate;
        this.jsonMapper = jsonMapper;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:5000}")
    public void publishPendingMessages() {
        for (OutboxMessage message : outboxService.loadPendingBatch(batchSize)) {
            if (!outboxService.markSending(message.getId())) {
                continue;
            }
            try {
                OrderPaidMessage payload = jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class);
                rocketMQTemplate.syncSend(message.getTopic(), payload);
                outboxService.markSent(message.getId());
            } catch (RuntimeException ex) {
                log.warn("publish inventory outbox failed, id={}", message.getId(), ex);
                outboxService.markFailed(message, ex.getMessage());
            }
        }
    }
}
