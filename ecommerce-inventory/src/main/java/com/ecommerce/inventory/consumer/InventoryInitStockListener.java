package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.ProductCreatedMessage;
import com.ecommerce.inventory.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "product-created", consumerGroup = "${spring.application.name}-init")
public class InventoryInitStockListener implements RocketMQListener<ProductCreatedMessage> {

    private final StockService stockService;

    public InventoryInitStockListener(StockService stockService) {
        this.stockService = stockService;
    }

    @Override
    public void onMessage(ProductCreatedMessage msg) {
        log.info("MQ init-stock: skuId={}", msg.getSkuId());
        try {
            stockService.setStock(msg.getSkuId(), 0);
            log.info("Stock initialized: skuId={}", msg.getSkuId());
        } catch (Exception e) {
            log.error("Init stock failed: skuId={}", msg.getSkuId(), e);
        }
    }
}
