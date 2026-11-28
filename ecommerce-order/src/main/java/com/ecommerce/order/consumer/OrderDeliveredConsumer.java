package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.order.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@RocketMQMessageListener(
    topic = "order-delivered",
    consumerGroup = "${spring.application.name}-order-delivered"
)
public class OrderDeliveredConsumer implements RocketMQListener<OrderDeliveredMessage> {

    private final OrderFulfillmentService fulfillment;

    @Override
    public void onMessage(OrderDeliveredMessage message) {
        fulfillment.advance(message.getOrderId(), true, message.isWholeOrder());
    }
}
