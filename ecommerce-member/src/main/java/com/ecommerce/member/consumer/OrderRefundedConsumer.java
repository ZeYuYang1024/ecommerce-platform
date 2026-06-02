package com.ecommerce.member.consumer;

import com.ecommerce.common.dto.OrderRefundedMessage;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.service.RefundCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "order-refunded",
        consumerGroup = "${spring.application.name}-refund-consumer"
)
public class OrderRefundedConsumer implements RocketMQListener<OrderRefundedMessage> {

    private final RefundCompensationService refundCompensationService;

    @Override
    public void onMessage(OrderRefundedMessage message) {
        log.info("Member MQ received: order-refunded, orderNo={}, refundNo={}",
                message.getOrderNo(), message.getRefundNo());

        refundCompensationService.compensate(new RefundCompensationRequest(
                message.getRefundNo(),
                message.getOrderNo(),
                message.getUserId(),
                message.getRefundAmount(),
                message.getRefundType(),
                message.getIdempotencyKey()));
    }
}
