package com.ecommerce.notification.consumer;

import com.ecommerce.common.dto.ShippingExceptionMessage;
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
    topic = "shipping-exception",
    consumerGroup = "${rocketmq.consumer.group}-shipping-exception"
)
public class ShippingExceptionConsumer implements RocketMQListener<ShippingExceptionMessage> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(ShippingExceptionMessage msg) {
        log.info("Shipping exception: shippingId={}, orderNo={}, desc={}", msg.getShippingId(), msg.getOrderNo(), msg.getExceptionDesc());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setParams(Map.of(
            "shippingId", String.valueOf(msg.getShippingId()),
            "orderNo", msg.getOrderNo(),
            "trackingNo", msg.getTrackingNo() != null ? msg.getTrackingNo() : "",
            "exceptionDesc", msg.getExceptionDesc() != null ? msg.getExceptionDesc() : "",
            "occurredAt", msg.getOccurredAt() != null ? msg.getOccurredAt().toString() : ""
        ));
        notificationService.send("SHIPPING_EXCEPTION", null, req);
    }
}
