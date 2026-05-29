package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.StockService;
import com.ecommerce.inventory.transaction.InventoryTransactionExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryMessageServiceImplTest {

    @Mock
    private InventoryEventLogMapper inventoryEventLogMapper;

    @Mock
    private StockService stockService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private InventoryTransactionExecutor transactionExecutor;

    @InjectMocks
    private InventoryMessageServiceImpl inventoryMessageService;

    private OrderInventoryMessage message;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionExecutor).execute(any(Runnable.class));

        message = new OrderInventoryMessage(
                "ORD-1001",
                "tx-order-1001",
                "order-created:ORD-1001",
                List.of(
                        new OrderItemMessage(11L, 2),
                        new OrderItemMessage(22L, 3)
                )
        );
    }

    @Test
    void handleDeductShouldSkipWhenMessageAlreadyProcessed() {
        doThrow(new DuplicateKeyException("duplicate")).when(inventoryEventLogMapper).insert(any(InventoryEventLog.class));

        inventoryMessageService.handleDeduct(message);

        verify(stockService, never()).deduct(any(Long.class), any(Integer.class));
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
        verify(transactionExecutor, never()).execute(any(Runnable.class));
    }

    @Test
    void handleDeductShouldProcessAllItemsAndMarkProcessedInSingleTransaction() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);

        inventoryMessageService.handleDeduct(message);

        verify(transactionExecutor, times(1)).execute(any(Runnable.class));
        verify(stockService).deduct(11L, 2);
        verify(stockService).deduct(22L, 3);
        verify(inventoryEventLogMapper).markProcessed(any(Long.class));
        verify(inventoryEventLogMapper, never()).markFailed(any(Long.class));
        verify(outboxService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void handleReleaseShouldProcessAllItemsAndMarkProcessed() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);

        inventoryMessageService.handleRelease(message);

        verify(stockService).release(11L, 2);
        verify(stockService).release(22L, 3);
        verify(inventoryEventLogMapper).markProcessed(any(Long.class));
    }

    @Test
    void handleReleaseShouldNotMarkProcessedWhenStockUpdateFails() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);
        doNothing().when(stockService).release(11L, 2);
        doThrow(new IllegalStateException("boom")).when(stockService).release(22L, 3);

        assertThatThrownBy(() -> inventoryMessageService.handleRelease(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(stockService).release(11L, 2);
        verify(stockService).release(22L, 3);
        verify(transactionExecutor, times(2)).execute(any(Runnable.class));
        verify(inventoryEventLogMapper).markFailed(any(Long.class));
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
    }

    @Test
    void handleDeductShouldEnqueueCompensationWhenStockDeductionFailsInSingleTransaction() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);
        doThrow(new IllegalStateException("stock down")).when(stockService).deduct(11L, 2);

        inventoryMessageService.handleDeduct(message);

        verify(transactionExecutor, times(1)).execute(any(Runnable.class));
        verify(outboxService).enqueue(eq("inventory"), eq("ORD-1001"), eq("order-paid"),
                org.mockito.ArgumentMatchers.argThat((OrderPaidMessage compensation) ->
                        compensation != null
                                && "ORD-1001".equals(compensation.getOrderNo())
                                && Integer.valueOf(4).equals(compensation.getStatus())
                                && "tx-order-1001".equals(compensation.getTransactionId())
                                && "order-created:ORD-1001:compensate".equals(compensation.getIdempotencyKey())
                                && "stock down".equals(compensation.getErrorMessage())));
        verify(inventoryEventLogMapper).markFailed(any(Long.class));
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
    }

    @Test
    void handleDeductShouldEnqueueCompensationWhenInventoryIsInsufficientInSingleTransaction() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);
        doThrow(new IllegalStateException("insufficient stock")).when(stockService).deduct(11L, 2);

        inventoryMessageService.handleDeduct(message);

        verify(transactionExecutor, times(1)).execute(any(Runnable.class));
        verify(outboxService).enqueue(eq("inventory"), eq("ORD-1001"), eq("order-paid"),
                org.mockito.ArgumentMatchers.argThat((OrderPaidMessage compensation) ->
                        compensation != null
                                && Integer.valueOf(4).equals(compensation.getStatus())
                                && "tx-order-1001".equals(compensation.getTransactionId())
                                && "order-created:ORD-1001:compensate".equals(compensation.getIdempotencyKey())
                                && "insufficient stock".equals(compensation.getErrorMessage())));
        verify(inventoryEventLogMapper).markFailed(any(Long.class));
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
    }
}
