package com.ecommerce.payment.outbox;

import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.common.outbox.OutboxMessage;
import com.ecommerce.common.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
public class PaymentOutboxPublisher {

    private final OutboxService outboxService;
    private final RocketMQTemplate rocketMQTemplate;
    private final JsonMapper jsonMapper;
    private final int batchSize;

    PaymentOutboxPublisher(OutboxService outboxService, RocketMQTemplate rocketMQTemplate,
                           JsonMapper jsonMapper) {
        this(outboxService, rocketMQTemplate, jsonMapper, 20);
    }

    @Autowired
    public PaymentOutboxPublisher(OutboxService outboxService, RocketMQTemplate rocketMQTemplate,
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
                Object payload = deserializePayload(message);
                rocketMQTemplate.syncSend(message.getTopic(), payload);
                outboxService.markSent(message.getId());
            } catch (RuntimeException e) {
                log.warn("publish payment outbox failed, id={}", message.getId(), e);
                outboxService.markFailed(message, e.getMessage());
            }
        }
    }

    private Object deserializePayload(OutboxMessage message) {
        if ("order-refunded".equals(message.getTopic())) {
            return jsonMapper.readValue(message.getPayloadJson(), OrderRefundedMessage.class);
        }
        return jsonMapper.readValue(message.getPayloadJson(), OrderPaidMessage.class);
    }
}
