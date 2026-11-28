package com.ecommerce.member.consumer;

import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.constant.OrderStatus;
import com.ecommerce.common.dto.OrderPaidMessage;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.service.MemberOrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "order-paid",
    consumerGroup = "${spring.application.name}-consumer"
)
public class OrderPaidConsumer implements RocketMQListener<OrderPaidMessage> {

    private final OrderClient orderClient;
    private final MemberOrderPaymentService payments;

    @Override
    public void onMessage(OrderPaidMessage message) {
        if (!Integer.valueOf(OrderStatus.PAID).equals(message.getStatus())) return;
        Result<OrderMemberVO> result = orderClient.getOrderForMember(message.getOrderNo());
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new IllegalStateException("Cannot load paid order for member benefits: " + message.getOrderNo());
        }
        payments.confirmPayment(result.getData());
    }
}
