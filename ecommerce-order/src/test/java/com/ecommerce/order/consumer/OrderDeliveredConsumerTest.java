package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import com.ecommerce.order.mapper.OrderItemMapper;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.service.OrderFulfillmentService;
import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.common.outbox.OutboxService;
import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeliveredConsumerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItems;

    @Mock
    private OutboxService outbox;

    private OrderDeliveredConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderDeliveredConsumer(new OrderFulfillmentService(orderMapper, orderItems, outbox));
    }

    @Test
    void completesConfirmedWholeOrderWhenSignedMessageArrivesBeforeDispatch() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(1);
        order.setInventoryReserved(true);
        OrderItem item = new OrderItem();
        item.setSkuId(11L);
        item.setQuantity(2);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.transitionStatus(1L, 1, 3)).thenReturn(true);
        when(orderItems.selectList(any())).thenReturn(List.of(item));

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1", LocalDateTime.now(), LocalDateTime.now(), true));

        verify(orderMapper).transitionStatus(1L, 1, 3);
        verify(outbox).enqueue(eq("order"), eq("ORD-1"), eq(OrderInventoryMessage.CONSUMED_TOPIC), any());
    }

    @Test
    void dispatchAfterRefundStillConsumesTheStockReservedForTheShipment() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(5);
        order.setInventoryReserved(true);
        OrderItem item = new OrderItem();
        item.setSkuId(11L);
        item.setQuantity(2);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItems.selectList(any())).thenReturn(List.of(item));
        ShippingDispatchedMessage message = new ShippingDispatchedMessage();
        message.setOrderId(1L);
        message.setWholeOrder(true);

        new ShippingDispatchedConsumer(new OrderFulfillmentService(orderMapper, orderItems, outbox)).onMessage(message);

        verify(orderMapper, never()).transitionStatus(any(), anyInt(), anyInt());
        verify(outbox).enqueue(eq("order"), eq("ORD-1"), eq(OrderInventoryMessage.CONSUMED_TOPIC), any());
    }

    @Test
    void shouldUpdateShippedOrderToCompleted() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(2);
        when(orderMapper.selectOne(any())).thenReturn(order);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper).transitionStatus(1L, 2, 3);
    }

    @Test
    void shouldIgnoreDeliveredMessageWhenOrderAlreadyCompleted() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(3);
        when(orderMapper.selectOne(any())).thenReturn(order);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper, never()).transitionStatus(any(), anyInt(), anyInt());
    }

    @Test
    void shouldIgnoreDeliveredMessageWhenOrderNotFound() {
        when(orderMapper.selectOne(any())).thenReturn(null);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper, never()).transitionStatus(any(), anyInt(), anyInt());
    }
}
