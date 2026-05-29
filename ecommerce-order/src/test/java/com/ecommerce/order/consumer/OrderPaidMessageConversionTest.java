package com.ecommerce.order.consumer;

import com.ecommerce.common.dto.OrderPaidMessage;
import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.support.MessageBuilder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaidMessageConversionTest {

    @Test
    void shouldConvertOrderPaidPayloadWithPlatformDateTimeFormat() {
        var converter = new RocketMQMessageConverter().getMessageConverter();

        OrderPaidMessage message = (OrderPaidMessage) converter.fromMessage(
                MessageBuilder.withPayload("""
                        {
                          "orderNo":"ORD-1",
                          "status":1,
                          "paidAt":"2026-05-29 06:48:59",
                          "transactionId":"TX-1",
                          "idempotencyKey":"payment-paid:ORD-1",
                          "errorMessage":null
                        }
                        """.getBytes()).build(),
                OrderPaidMessage.class);

        assertThat(message.getOrderNo()).isEqualTo("ORD-1");
        assertThat(message.getStatus()).isEqualTo(1);
        assertThat(message.getPaidAt()).isEqualTo(LocalDateTime.of(2026, 5, 29, 6, 48, 59));
        assertThat(message.getTransactionId()).isEqualTo("TX-1");
        assertThat(message.getIdempotencyKey()).isEqualTo("payment-paid:ORD-1");
    }
}
