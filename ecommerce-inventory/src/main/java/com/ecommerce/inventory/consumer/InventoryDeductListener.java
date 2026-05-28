package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.inventory.service.InventoryMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-created", consumerGroup = "${spring.application.name}-deduct")
public class InventoryDeductListener implements RocketMQListener<OrderInventoryMessage> {

    private final InventoryMessageService inventoryMessageService;

    public InventoryDeductListener(InventoryMessageService inventoryMessageService) {
        this.inventoryMessageService = inventoryMessageService;
    }

    @Override
    public void onMessage(OrderInventoryMessage msg) {
        log.info("MQ deduct: orderNo={} items={}", msg.getOrderNo(), msg.getItems().size());
        inventoryMessageService.handleDeduct(msg);
    }
}
