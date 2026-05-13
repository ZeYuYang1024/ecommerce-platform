package com.ecommerce.notification.consumer;

import com.ecommerce.common.dto.MerchantApprovedMessage;
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
    topic = "merchant-approved",
    consumerGroup = "${rocketmq.consumer.group}-merchant-approved"
)
public class MerchantApprovedConsumer implements RocketMQListener<MerchantApprovedMessage> {

    private final NotificationService notificationService;

    @Override
    public void onMessage(MerchantApprovedMessage msg) {
        log.info("Merchant approved: merchantId={}", msg.getMerchantId());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setParams(Map.of(
            "merchantId", String.valueOf(msg.getMerchantId()),
            "merchantName", msg.getMerchantName()
        ));
        notificationService.send("MERCHANT_APPROVED", null, req);
    }
}
