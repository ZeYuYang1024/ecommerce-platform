package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.order.service.OrderFulfillmentService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@RocketMQMessageListener(
    topic = "shipping-dispatched",
    consumerGroup = "${spring.application.name}-shipping-dispatched"
)
public class ShippingDispatchedConsumer implements RocketMQListener<ShippingDispatchedMessage> {

    private final OrderFulfillmentService fulfillment;

    @Override
    public void onMessage(ShippingDispatchedMessage message) {
        fulfillment.advance(message.getOrderId(), false, message.isWholeOrder());
    }
}
