package com.ecommerce.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-paid",
    consumerGroup = "${spring.application.name}-order-paid"
)
public class OrderPaidConsumer implements RocketMQListener<OrderPaidMessage> {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public OrderPaidConsumer(OrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Override
    public void onMessage(OrderPaidMessage message) {
        log.info("MQ received: order-paid, orderNo={}", message.getOrderNo());
        Order order = orderMapper.selectOne(
            new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, message.getOrderNo()));
        if (order == null) {
            log.warn("Order not found: {}", message.getOrderNo());
            return;
        }
        if (Integer.valueOf(OrderStatus.CANCELLED).equals(message.getStatus())) {
            orderService.applyInventoryCompensation(message);
        } else if (Integer.valueOf(OrderStatus.PAID).equals(message.getStatus())
                && (Integer.valueOf(OrderStatus.PENDING).equals(order.getStatus())
                    || Integer.valueOf(OrderStatus.CANCELLED).equals(order.getStatus()))
                && orderService.confirmPayment(order, message)) {
            log.info("Order status updated: {} -> {}", message.getOrderNo(), OrderStatus.PAID);
        }
    }
}
