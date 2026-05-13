package com.ecommerce.notification.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.notification.dto.request.SendNotificationRequest;
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
    topic = "order-cancelled",
    consumerGroup = "${rocketmq.consumer.group}-order-cancelled"
)
public class OrderCancelledConsumer implements RocketMQListener<OrderInventoryMessage> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(OrderInventoryMessage msg) {
        log.info("Order cancelled: orderNo={}", msg.getOrderNo());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setParams(Map.of("orderNo", msg.getOrderNo()));
        notificationService.send("ORDER_CANCELLED", null, req);
    }
}
