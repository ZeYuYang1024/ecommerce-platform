package com.ecommerce.common.dto;

import com.ecommerce.common.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRefundedMessageTest {

    @Test
    void shouldSerializeAndDeserializeRefundedMessage() throws Exception {
        JacksonConfig config = new JacksonConfig();
        ReflectionTestUtils.setField(config, "dateTimeFormat", "yyyy-MM-dd HH:mm:ss");
        ReflectionTestUtils.setField(config, "dateFormat", "yyyy-MM-dd");
        ReflectionTestUtils.setField(config, "timeFormat", "HH:mm:ss");
        ReflectionTestUtils.setField(config, "timeZone", "Asia/Shanghai");
        JsonMapper objectMapper = ReflectionTestUtils.invokeMethod(config, "jsonMapper");

        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 2, 13, 10, 0);
        OrderRefundedMessage message = new OrderRefundedMessage(
                "RF202606020001",
                "ORD202606020001",
                10001L,
                new BigDecimal("50.00"),
                "PARTIAL",
                "SUCCESS",
                occurredAt,
                "payment-refund:ORD202606020001:RF202606020001");

        String json = objectMapper.writeValueAsString(message);
        OrderRefundedMessage decoded = objectMapper.readValue(json, OrderRefundedMessage.class);

        assertThat(json).contains("\"occurredAt\":\"2026-06-02 13:10:00\"");
        assertThat(decoded.getRefundNo()).isEqualTo("RF202606020001");
        assertThat(decoded.getOrderNo()).isEqualTo("ORD202606020001");
        assertThat(decoded.getUserId()).isEqualTo(10001L);
        assertThat(decoded.getRefundAmount()).isEqualByComparingTo("50.00");
        assertThat(decoded.getRefundType()).isEqualTo("PARTIAL");
        assertThat(decoded.getRefundStatus()).isEqualTo("SUCCESS");
        assertThat(decoded.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(decoded.getIdempotencyKey()).isEqualTo("payment-refund:ORD202606020001:RF202606020001");
    }
}
