package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.inventory.service.InventoryMessageService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "after-sale-stock-release", consumerGroup = "${spring.application.name}-after-sale-release")
public class AfterSaleStockReleaseListener implements RocketMQListener<OrderInventoryMessage> {
    private final InventoryMessageService service;

    @Override
    public void onMessage(OrderInventoryMessage message) {
        service.handleRelease(message);
    }
}
