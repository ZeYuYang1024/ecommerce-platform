package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.InventoryMessageService;
import com.ecommerce.inventory.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class InventoryMessageServiceImpl implements InventoryMessageService {

    private static final int STATUS_PROCESSING = 0;

    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String TOPIC_ORDER_CANCELLED = "order-cancelled";

    private final InventoryEventLogMapper inventoryEventLogMapper;
    private final StockService stockService;

    public InventoryMessageServiceImpl(InventoryEventLogMapper inventoryEventLogMapper, StockService stockService) {
        this.inventoryEventLogMapper = inventoryEventLogMapper;
        this.stockService = stockService;
    }

    @Transactional
    @Override
    public void handleDeduct(OrderInventoryMessage message) {
        handle(message, TOPIC_ORDER_CREATED, Operation.DEDUCT);
    }

    @Transactional
    @Override
    public void handleRelease(OrderInventoryMessage message) {
        handle(message, TOPIC_ORDER_CANCELLED, Operation.RELEASE);
    }

    private void handle(OrderInventoryMessage message, String topic, Operation operation) {
        Long eventLogId = tryCreateProcessingLog(topic, message.getOrderNo());
        if (eventLogId == null) {
            log.info("Skip duplicated inventory event: topic={} orderNo={}", topic, message.getOrderNo());
            return;
        }
        for (OrderItemMessage item : message.getItems()) {
            operation.apply(stockService, item);
        }
        inventoryEventLogMapper.markProcessed(eventLogId);
    }

    private Long tryCreateProcessingLog(String topic, String orderNo) {
        InventoryEventLog eventLog = new InventoryEventLog();
        eventLog.setId(SnowflakeUtils.nextId());
        eventLog.setTopic(topic);
        eventLog.setOrderNo(orderNo);
        eventLog.setStatus(STATUS_PROCESSING);
        try {
            inventoryEventLogMapper.insert(eventLog);
            return eventLog.getId();
        } catch (DuplicateKeyException ex) {
            return null;
        }
    }

    private enum Operation {
        DEDUCT {
            @Override
            void apply(StockService stockService, OrderItemMessage item) {
                stockService.deduct(item.getSkuId(), item.getQuantity());
            }
        },
        RELEASE {
            @Override
            void apply(StockService stockService, OrderItemMessage item) {
                stockService.release(item.getSkuId(), item.getQuantity());
            }
        };

        abstract void apply(StockService stockService, OrderItemMessage item);
    }
}
