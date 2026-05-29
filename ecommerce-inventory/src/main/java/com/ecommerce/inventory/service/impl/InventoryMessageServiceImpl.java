package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.InventoryMessageService;
import com.ecommerce.inventory.service.StockService;
import com.ecommerce.inventory.transaction.InventoryTransactionExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryMessageServiceImpl implements InventoryMessageService {

    private static final int STATUS_PROCESSING = 0;

    private static final String TOPIC_ORDER_CREATED = "order-created";
    private static final String TOPIC_ORDER_CANCELLED = "order-cancelled";
    private static final String TOPIC_ORDER_PAID = "order-paid";

    private final InventoryEventLogMapper inventoryEventLogMapper;
    private final StockService stockService;
    private final OutboxService outboxService;
    private final InventoryTransactionExecutor transactionExecutor;

    public InventoryMessageServiceImpl(InventoryEventLogMapper inventoryEventLogMapper,
                                       StockService stockService,
                                       OutboxService outboxService,
                                       InventoryTransactionExecutor transactionExecutor) {
        this.inventoryEventLogMapper = inventoryEventLogMapper;
        this.stockService = stockService;
        this.outboxService = outboxService;
        this.transactionExecutor = transactionExecutor;
    }

    @Override
    public void handleDeduct(OrderInventoryMessage message) {
        handle(message, TOPIC_ORDER_CREATED, Operation.DEDUCT);
    }

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
        if (TOPIC_ORDER_CREATED.equals(topic)) {
            // Keep failure mark and compensation outbox in one local transaction / 在同一本地事务里同时落失败标记和补偿 outbox
            transactionExecutor.execute(() -> {
                try {
                    for (OrderItemMessage item : message.getItems()) {
                        operation.apply(stockService, item);
                    }
                    inventoryEventLogMapper.markProcessed(eventLogId);
                } catch (RuntimeException ex) {
                    inventoryEventLogMapper.markFailed(eventLogId);
                    publishInventoryCompensation(message, ex);
                    log.warn("Inventory deduct failed, compensation queued: orderNo={}", message.getOrderNo(), ex);
                }
            });
            return;
        }
        try {
            transactionExecutor.execute(() -> {
                for (OrderItemMessage item : message.getItems()) {
                    operation.apply(stockService, item);
                }
                inventoryEventLogMapper.markProcessed(eventLogId);
            });
        } catch (RuntimeException ex) {
            transactionExecutor.execute(() -> inventoryEventLogMapper.markFailed(eventLogId));
            throw ex;
        }
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

    private void publishInventoryCompensation(OrderInventoryMessage message, RuntimeException ex) {
        outboxService.enqueue("inventory", message.getOrderNo(), TOPIC_ORDER_PAID,
                new OrderPaidMessage(
                        message.getOrderNo(),
                        4,
                        null,
                        message.getTransactionId(),
                        compensationIdempotencyKey(message),
                        ex.getMessage()));
    }

    private String compensationIdempotencyKey(OrderInventoryMessage message) {
        if (message.getIdempotencyKey() == null || message.getIdempotencyKey().isBlank()) {
            return "inventory-compensation:" + message.getOrderNo();
        }
        return message.getIdempotencyKey() + ":compensate";
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
