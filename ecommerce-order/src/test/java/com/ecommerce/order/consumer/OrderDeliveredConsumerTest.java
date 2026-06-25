package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDeliveredConsumerTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderDeliveredConsumer consumer;

    @Test
    void shouldUpdateShippedOrderToCompleted() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(2);
        when(orderMapper.selectById(1L)).thenReturn(order);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper).updateById(order);
    }

    @Test
    void shouldIgnoreDeliveredMessageWhenOrderAlreadyCompleted() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(3);
        when(orderMapper.selectById(1L)).thenReturn(order);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper, never()).updateById(order);
    }

    @Test
    void shouldIgnoreDeliveredMessageWhenOrderNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        consumer.onMessage(new OrderDeliveredMessage(
                10L, 1L, "ORD-1", null, "tx-1", "idem-1",
                LocalDateTime.now(), LocalDateTime.now()));

        verify(orderMapper, never()).updateById(org.mockito.ArgumentMatchers.any(Order.class));
    }
}
