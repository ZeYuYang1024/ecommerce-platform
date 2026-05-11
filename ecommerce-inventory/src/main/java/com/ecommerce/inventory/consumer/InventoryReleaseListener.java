package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.inventory.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "order-cancelled", consumerGroup = "${spring.application.name}-release")
public class InventoryReleaseListener implements RocketMQListener<OrderInventoryMessage> {

    private final StockService stockService;

    public InventoryReleaseListener(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void onMessage(OrderInventoryMessage msg) {
        log.info("MQ release: orderNo={} items={}", msg.getOrderNo(), msg.getItems().size());
        for (OrderItemMessage item : msg.getItems()) {
            stockService.release(item.getSkuId(), item.getQuantity());
            log.info("Released: skuId={} qty={}", item.getSkuId(), item.getQuantity());
        }
    }
}
