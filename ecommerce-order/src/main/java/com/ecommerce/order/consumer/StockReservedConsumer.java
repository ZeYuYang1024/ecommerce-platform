package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = OrderInventoryMessage.RESERVED_TOPIC,
        consumerGroup = "${spring.application.name}-stock-reserved")
public class StockReservedConsumer implements RocketMQListener<OrderInventoryMessage> {
    private final OrderMapper orders;

    @Override
    public void onMessage(OrderInventoryMessage message) {
        orders.markInventoryReserved(message.getOrderNo());
    }
}
