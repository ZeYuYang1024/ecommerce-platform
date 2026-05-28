package com.ecommerce.inventory.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.inventory.service.InventoryMessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryReleaseListenerTest {

    @Mock
    private InventoryMessageService inventoryMessageService;

    @Test
    void onMessage_shouldDelegateToInventoryMessageService() {
        InventoryReleaseListener listener = new InventoryReleaseListener(inventoryMessageService);
        OrderInventoryMessage message = new OrderInventoryMessage("ORD-1001", List.of(new OrderItemMessage(11L, 2)));

        listener.onMessage(message);

        verify(inventoryMessageService).handleRelease(message);
    }
}
