package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.inventory.common.InventoryErrorCode;
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
        String orderNo = message.getOrderNo();
        try {
            transactionExecutor.execute(() -> {
                inventoryEventLogMapper.lockOrder(orderNo);
                Long eventLogId = tryCreateProcessingLog(TOPIC_ORDER_CREATED, orderNo);
                if (eventLogId == null) return;
                if (inventoryEventLogMapper.wasProcessed(orderNo, TOPIC_ORDER_CANCELLED)) {
                    // A release that arrived first cancels this reservation without changing stock.
                    inventoryEventLogMapper.markFailed(eventLogId);
                    return;
                }
                for (OrderItemMessage item : message.getItems()) {
                    stockService.deduct(item.getSkuId(), item.getQuantity());
                }
                inventoryEventLogMapper.markProcessed(eventLogId);
            });
        } catch (BusinessException error) {
            if (error.getErrorCode() != InventoryErrorCode.STOCK_INSUFFICIENT) throw error;
            compensateInsufficientStock(message, error);
        }
    }

    private void compensateInsufficientStock(OrderInventoryMessage message, BusinessException error) {
        String orderNo = message.getOrderNo();
        // Roll back all SKU changes first, then persist the failure and its compensation together.
        transactionExecutor.execute(() -> {
            inventoryEventLogMapper.lockOrder(orderNo);
            Long eventLogId = tryCreateProcessingLog(TOPIC_ORDER_CREATED, orderNo);
            if (eventLogId == null) return;
            inventoryEventLogMapper.markFailed(eventLogId);
            if (!inventoryEventLogMapper.wasProcessed(orderNo, TOPIC_ORDER_CANCELLED)) {
                publishInventoryCompensation(message, error);
            }
        });
    }

    @Override
    public void handleRelease(OrderInventoryMessage message) {
        String orderNo = message.getOrderNo();
        transactionExecutor.execute(() -> {
            inventoryEventLogMapper.lockOrder(orderNo);
            Long eventLogId = tryCreateProcessingLog(TOPIC_ORDER_CANCELLED, orderNo);
            if (eventLogId == null) return;
            if (inventoryEventLogMapper.wasProcessed(orderNo, TOPIC_ORDER_CREATED)) {
                for (OrderItemMessage item : message.getItems()) {
                    stockService.release(item.getSkuId(), item.getQuantity());
                }
            }
            inventoryEventLogMapper.markProcessed(eventLogId);
        });
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
            log.info("Skip duplicated inventory event: topic={} orderNo={}", topic, orderNo);
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
}
