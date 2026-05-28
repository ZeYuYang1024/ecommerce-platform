package com.ecommerce.inventory.service.impl;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.inventory.entity.InventoryEventLog;
import com.ecommerce.inventory.mapper.InventoryEventLogMapper;
import com.ecommerce.inventory.service.StockService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    @InjectMocks
    private InventoryMessageServiceImpl inventoryMessageService;

    private OrderInventoryMessage message;

    @BeforeEach
    void setUp() {
        message = new OrderInventoryMessage(
                "ORD-1001",
                List.of(
                        new OrderItemMessage(11L, 2),
                        new OrderItemMessage(22L, 3)
                )
        );
    }

    @Test
    void handleDeduct_shouldSkipWhenMessageAlreadyProcessed() {
        doThrow(new DuplicateKeyException("duplicate")).when(inventoryEventLogMapper).insert(any(InventoryEventLog.class));

        inventoryMessageService.handleDeduct(message);

        verify(stockService, never()).deduct(any(Long.class), any(Integer.class));
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
    }

    @Test
    void handleDeduct_shouldProcessAllItemsAndMarkProcessed() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);

        inventoryMessageService.handleDeduct(message);

        verify(stockService).deduct(11L, 2);
        verify(stockService).deduct(22L, 3);
        verify(inventoryEventLogMapper, times(1)).markProcessed(any(Long.class));
    }

    @Test
    void handleRelease_shouldProcessAllItemsAndMarkProcessed() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);

        inventoryMessageService.handleRelease(message);

        verify(stockService).release(11L, 2);
        verify(stockService).release(22L, 3);
        verify(inventoryEventLogMapper, times(1)).markProcessed(any(Long.class));
    }

    @Test
    void handleRelease_shouldNotMarkProcessedWhenStockUpdateFails() {
        when(inventoryEventLogMapper.insert(any(InventoryEventLog.class))).thenReturn(1);
        doNothing().when(stockService).release(11L, 2);
        doThrow(new IllegalStateException("boom")).when(stockService).release(22L, 3);

        assertThatThrownBy(() -> inventoryMessageService.handleRelease(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        verify(stockService).release(11L, 2);
        verify(stockService).release(22L, 3);
        verify(inventoryEventLogMapper, never()).markProcessed(any(Long.class));
    }
}
