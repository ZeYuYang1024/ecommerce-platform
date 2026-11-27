package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartItem;
import com.ecommerce.common.dto.RecommendationSignal;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CartRecommendationPublisher {
    private final RocketMQTemplate rocketMQ;

    public CartRecommendationPublisher(RocketMQTemplate rocketMQ) {
        this.rocketMQ = rocketMQ;
    }

    public void added(Long userId, CartItem item) {
        if (item.getSpuId() == null || item.getSkuId() == null) return;
        var signal = new RecommendationSignal(UUID.randomUUID().toString(), RecommendationSignal.Type.CART_ADD,
                userId, List.of(item.getSpuId()), item.getSkuId(), System.currentTimeMillis());
        try {
            rocketMQ.asyncSend(RecommendationSignal.TOPIC, signal, new SendCallback() {
                @Override
                public void onSuccess(SendResult result) {}

                @Override
                public void onException(Throwable error) {
                    log.warn("Could not publish cart recommendation event {}", signal.eventId(), error);
                }
            }, 500);
        } catch (RuntimeException error) {
            log.warn("Could not queue cart recommendation event {}", signal.eventId(), error);
        }
    }
}
