package com.ecommerce.payment.consumer;

import com.ecommerce.common.dto.AfterSaleRefundRequest;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = AfterSaleRefundRequest.TOPIC, consumerGroup = "${spring.application.name}-after-sale")
public class AfterSaleRefundRequestConsumer implements RocketMQListener<AfterSaleRefundRequest> {
    private final PaymentService service;

    @Override
    public void onMessage(AfterSaleRefundRequest message) {
        service.refundAfterSale(message);
    }
}
