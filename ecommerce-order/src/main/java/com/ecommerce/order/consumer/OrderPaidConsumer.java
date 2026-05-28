package com.ecommerce.order.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-paid",
    consumerGroup = "${spring.application.name}-consumer"
)
public class OrderPaidConsumer implements RocketMQListener<OrderPaidMessage> {

    private final OrderMapper orderMapper;

    public OrderPaidConsumer(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
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
        if (order.getStatus() != null && order.getStatus() == message.getStatus()) {
            log.info("Order already in status {}: {}", message.getStatus(), message.getOrderNo());
            return;
        }
        if (!shouldApply(order.getStatus(), message.getStatus())) {
            log.warn("Ignore incompatible order status update: orderNo={}, currentStatus={}, incomingStatus={}",
                    message.getOrderNo(), order.getStatus(), message.getStatus());
            return;
        }
        order.setStatus(message.getStatus());
        orderMapper.updateById(order);
        log.info("Order status updated: {} -> {}", message.getOrderNo(), message.getStatus());
    }

    private boolean shouldApply(Integer currentStatus, Integer incomingStatus) {
        if (currentStatus == null || incomingStatus == null) {
            return false;
        }
        if (incomingStatus == 1) {
            return currentStatus == 0;
        }
        if (incomingStatus == 5) {
            return currentStatus == 1;
        }
        return false;
    }
}
