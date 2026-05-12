package com.ecommerce.notification.consumer;

import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "order-paid",
    consumerGroup = "${rocketmq.consumer.group}-order-paid"
)
public class OrderPaidConsumer implements RocketMQListener<OrderPaidMessage> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(OrderPaidMessage msg) {
        log.info("Order paid: orderNo={}", msg.getOrderNo());
        notificationService.send("ORDER_PAID", null, Map.of(
            "orderNo", msg.getOrderNo(),
            "paidAt", msg.getPaidAt() != null ? msg.getPaidAt().toString() : ""
        ));
    }
}
