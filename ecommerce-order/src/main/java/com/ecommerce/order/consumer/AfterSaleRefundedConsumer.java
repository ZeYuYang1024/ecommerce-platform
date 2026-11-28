package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.order.service.AfterSaleService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "order-refunded", consumerGroup = "${spring.application.name}-after-sale")
public class AfterSaleRefundedConsumer implements RocketMQListener<OrderRefundedMessage> {
    private final AfterSaleService service;

    @Override
    public void onMessage(OrderRefundedMessage message) {
        service.finish(message);
    }
}
