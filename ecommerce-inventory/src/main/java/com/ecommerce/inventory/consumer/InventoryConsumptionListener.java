package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.inventory.service.InventoryMessageService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = OrderInventoryMessage.CONSUMED_TOPIC,
        consumerGroup = "${spring.application.name}-stock-consumed")
public class InventoryConsumptionListener implements RocketMQListener<OrderInventoryMessage> {
    private final InventoryMessageService inventory;

    @Override
    public void onMessage(OrderInventoryMessage message) {
        inventory.handleConsumption(message);
    }
}
