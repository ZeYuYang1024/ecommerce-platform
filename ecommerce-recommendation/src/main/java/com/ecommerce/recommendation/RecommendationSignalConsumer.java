package com.ecommerce.recommendation;

import com.ecommerce.common.dto.RecommendationSignal;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(topic = RecommendationSignal.TOPIC, consumerGroup = "ecommerce-recommendation-signals")
class RecommendationSignalConsumer implements RocketMQListener<RecommendationSignal> {
    private final RecommendationService service;

    RecommendationSignalConsumer(RecommendationService service) {
        this.service = service;
    }

    @Override
    public void onMessage(RecommendationSignal signal) {
        if (signal.type() == null || signal.eventId() == null || signal.userId() == null
                || signal.spuIds() == null || signal.occurredAtMillis() <= 0) {
            log.warn("Ignoring malformed recommendation signal");
            return;
        }
        service.recordSignal(signal);
    }
}
