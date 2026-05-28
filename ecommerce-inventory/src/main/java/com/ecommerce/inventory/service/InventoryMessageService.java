package com.ecommerce.inventory.service;

import com.ecommerce.common.dto.OrderInventoryMessage;

public interface InventoryMessageService {

    void handleDeduct(OrderInventoryMessage message);

    void handleRelease(OrderInventoryMessage message);
}
