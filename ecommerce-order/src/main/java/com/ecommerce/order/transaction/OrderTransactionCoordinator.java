package com.ecommerce.order.transaction;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.transaction.DistributedTransactionContext;
import com.ecommerce.common.transaction.DistributedTransactionEvent;
import com.ecommerce.common.util.SnowflakeUtils;

import java.util.List;

public final class OrderTransactionCoordinator {

    private OrderTransactionCoordinator() {
    }

    public static DistributedTransactionContext startOrderCreated(String orderNo) {
        DistributedTransactionContext context = DistributedTransactionContext.start(
                SnowflakeUtils.nextIdStr(),
                orderNo,
                "order-created:" + orderNo);
        context.apply(DistributedTransactionEvent.BEGIN, "order-created", null);
        return context;
    }

    public static OrderInventoryMessage buildInventoryMessage(DistributedTransactionContext context,
                                                              List<OrderItemMessage> items) {
        return new OrderInventoryMessage(
                context.getBusinessNo(),
                context.getTransactionId(),
                context.getIdempotencyKey(),
                items);
    }

    public static boolean shouldApplyInventoryCompensation(Integer currentStatus, Integer incomingStatus) {
        return currentStatus != null
                && incomingStatus != null
                && currentStatus == 0
                && incomingStatus == 4;
    }
}
