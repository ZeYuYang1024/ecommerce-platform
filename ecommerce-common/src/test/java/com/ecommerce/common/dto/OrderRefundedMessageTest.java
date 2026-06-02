package com.ecommerce.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRefundedMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldSerializeAndDeserializeRefundedMessage() throws Exception {
        OrderRefundedMessage message = new OrderRefundedMessage(
                "RF202606020001",
                "ORD202606020001",
                10001L,
                new BigDecimal("50.00"),
                "PARTIAL",
                "SUCCESS",
                LocalDateTime.of(2026, 6, 2, 13, 10, 0),
                "payment-refund:ORD202606020001:RF202606020001");

        String json = objectMapper.writeValueAsString(message);
        OrderRefundedMessage decoded = objectMapper.readValue(json, OrderRefundedMessage.class);

        assertThat(decoded.getRefundNo()).isEqualTo("RF202606020001");
        assertThat(decoded.getOrderNo()).isEqualTo("ORD202606020001");
        assertThat(decoded.getUserId()).isEqualTo(10001L);
        assertThat(decoded.getRefundAmount()).isEqualByComparingTo("50.00");
        assertThat(decoded.getRefundType()).isEqualTo("PARTIAL");
        assertThat(decoded.getRefundStatus()).isEqualTo("SUCCESS");
        assertThat(decoded.getIdempotencyKey()).isEqualTo("payment-refund:ORD202606020001:RF202606020001");
    }
}
