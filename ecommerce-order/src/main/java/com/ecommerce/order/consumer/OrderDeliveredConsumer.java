package com.ecommerce.order.consumer;

import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.OrderDeliveredMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "order-delivered",
    consumerGroup = "${spring.application.name}-consumer"
)
public class OrderDeliveredConsumer implements RocketMQListener<OrderDeliveredMessage> {

    private final OrderMapper orderMapper;

    public OrderDeliveredConsumer(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void onMessage(OrderDeliveredMessage message) {
        log.info("MQ received: order-delivered, orderId={}", message.getOrderId());
        Order order = orderMapper.selectById(message.getOrderId());
        if (order == null) {
            log.warn("Order not found: orderId={}", message.getOrderId());
            return;
        }
        if (order.getStatus() != null && order.getStatus() == OrderStatus.SHIPPED) {
            order.setStatus(OrderStatus.COMPLETED);
            orderMapper.updateById(order);
            log.info("Order status updated to completed: orderId={}", message.getOrderId());
        } else {
            log.info("Order not in shipped status, skip: orderId={}, status={}",
                    message.getOrderId(), order.getStatus());
        }
    }
}
