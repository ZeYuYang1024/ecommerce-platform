package com.ecommerce.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaidConsumerTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderPaidConsumer consumer;

    @Test
    void shouldUpdatePendingOrderToPaid() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(0);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        consumer.onMessage(new OrderPaidMessage("ORD-1", 1, LocalDateTime.now()));

        verify(orderMapper).updateById(order);
    }

    @Test
    void shouldIgnorePaidMessageWhenOrderAlreadyShipped() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(2);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        consumer.onMessage(new OrderPaidMessage("ORD-1", 1, LocalDateTime.now()));

        verify(orderMapper, never()).updateById(order);
    }

    @Test
    void shouldIgnoreLegacyRefundedMessageStatus() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(1);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        consumer.onMessage(new OrderPaidMessage("ORD-1", 5, LocalDateTime.now()));

        verify(orderMapper, never()).updateById(order);
    }

    @Test
    void shouldUpdatePendingOrderToCancelledWhenInventoryCompensationArrives() {
        Order order = new Order();
        order.setId(1L);
        order.setOrderNo("ORD-1");
        order.setStatus(0);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        consumer.onMessage(new OrderPaidMessage("ORD-1", 4, LocalDateTime.now()));

        verify(orderService).applyInventoryCompensation(any(OrderPaidMessage.class));
        verify(orderMapper, never()).updateById(order);
    }
}
