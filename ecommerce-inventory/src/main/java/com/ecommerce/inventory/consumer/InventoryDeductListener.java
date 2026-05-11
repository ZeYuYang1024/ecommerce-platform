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
@RocketMQMessageListener(topic = "order-created", consumerGroup = "${spring.application.name}-v2")
public class InventoryDeductListener implements RocketMQListener<OrderInventoryMessage> {

    private final StockService stockService;

    public InventoryDeductListener(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void onMessage(OrderInventoryMessage msg) {
        log.info("MQ deduct: orderNo={} items={}", msg.getOrderNo(), msg.getItems().size());
        for (OrderItemMessage item : msg.getItems()) {
            try {
                stockService.deduct(item.getSkuId(), item.getQuantity());
                log.info("Deducted: skuId={} qty={}", item.getSkuId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Deduct failed: skuId={}", item.getSkuId(), e);
            }
        }
    }
}
