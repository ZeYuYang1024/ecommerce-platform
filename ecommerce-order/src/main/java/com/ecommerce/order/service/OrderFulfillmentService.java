package com.ecommerce.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.OrderItemMessage;
import com.ecommerce.common.outbox.OutboxService;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFulfillmentService {
    private final OrderMapper orders;
    private final OrderItemMapper items;
    private final OutboxService outbox;

    @Transactional
    public void advance(Long orderId, boolean delivered, boolean wholeOrder) {
        if (orderId == null) return;
        Order order = orders.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, orderId).last("FOR UPDATE"));
        if (order == null || order.getStatus() == null) return;
        int current = order.getStatus();
        if (delivered && current == OrderStatus.SHIPPED) {
            orders.transitionStatus(orderId, current, OrderStatus.COMPLETED);
            return;
        }
        if (current == OrderStatus.REFUNDED && wholeOrder && Boolean.TRUE.equals(order.getInventoryReserved())) {
            // A manual refund can arrive before dispatch notification after the shipment already exists.
            publishStockConsumption(order);
            return;
        }
        if (current != OrderStatus.PAID || (delivered && !wholeOrder)) return;
        if (wholeOrder && !Boolean.TRUE.equals(order.getInventoryReserved())) {
            throw new IllegalStateException("Shipment has no confirmed inventory reservation: " + order.getOrderNo());
        }
        int next = delivered ? OrderStatus.COMPLETED : OrderStatus.SHIPPED;
        if (orders.transitionStatus(orderId, current, next) && wholeOrder) {
            publishStockConsumption(order);
        }
    }

    private void publishStockConsumption(Order order) {
        List<OrderItemMessage> shipped = items.selectList(new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, order.getId())).stream()
                .map(item -> new OrderItemMessage(item.getSkuId(), item.getQuantity())).toList();
        if (shipped.isEmpty()) throw new IllegalStateException("No items for shipped order " + order.getOrderNo());
        outbox.enqueue("order", order.getOrderNo(), OrderInventoryMessage.CONSUMED_TOPIC,
                new OrderInventoryMessage(order.getOrderNo(), shipped));
    }
}
