package com.ecommerce.member.consumer;

import com.ecommerce.common.dto.OrderInventoryMessage;
import com.ecommerce.member.service.MemberOrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "order-cancelled", consumerGroup = "${spring.application.name}-order-cancelled")
public class OrderCancelledConsumer implements RocketMQListener<OrderInventoryMessage> {
    private final MemberOrderPaymentService payments;

    @Override
    public void onMessage(OrderInventoryMessage message) {
        payments.cancel(message.getOrderNo());
    }
}
