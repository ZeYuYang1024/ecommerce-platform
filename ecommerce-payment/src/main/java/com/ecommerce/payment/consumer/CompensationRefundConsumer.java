package com.ecommerce.payment.consumer;

import com.ecommerce.common.dto.CompensationRefundRequest;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = CompensationRefundRequest.TOPIC,
        consumerGroup = "${spring.application.name}-compensation-refund")
public class CompensationRefundConsumer implements RocketMQListener<CompensationRefundRequest> {
    private final PaymentService payments;

    @Override
    public void onMessage(CompensationRefundRequest request) {
        try {
            payments.refundCompensation(request);
        } catch (RuntimeException error) {
            log.error("Compensation refund needs retry for orderNo={}", request.orderNo(), error);
            throw error;
        }
    }
}
