package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.ShippingDispatchedMessage;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "shipping-dispatched",
    consumerGroup = "${spring.application.name}-consumer"
)
public class ShippingDispatchedConsumer implements RocketMQListener<ShippingDispatchedMessage> {

    private final OrderMapper orderMapper;

    public ShippingDispatchedConsumer(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void onMessage(ShippingDispatchedMessage message) {
        log.info("MQ received: shipping-dispatched, orderId={}, shippingId={}", message.getOrderId(), message.getShippingId());
        Order order = orderMapper.selectById(message.getOrderId());
        if (order == null) {
            log.warn("Order not found: orderId={}", message.getOrderId());
            return;
        }
        // status=1 (paid) -> status=2 (shipped)
        if (order.getStatus() != null && order.getStatus() == 1) {
            order.setStatus(2);
            orderMapper.updateById(order);
            log.info("Order status updated to shipped: orderId={}", message.getOrderId());
        } else {
            log.info("Order already shipped or in incompatible status: orderId={}, status={}",
                    message.getOrderId(), order.getStatus());
        }
    }
}
