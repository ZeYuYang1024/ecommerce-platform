package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
public class OrderRefundedMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter OCCURRED_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String refundNo;
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundType;
    private String refundStatus;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @JsonProperty("occurredAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String occurredAtText;
    private String idempotencyKey;

    public OrderRefundedMessage(String refundNo, String orderNo, Long userId, BigDecimal refundAmount,
                                String refundType, String refundStatus, LocalDateTime occurredAt,
                                String idempotencyKey) {
        this.refundNo = refundNo;
        this.orderNo = orderNo;
        this.userId = userId;
        this.refundAmount = refundAmount;
        this.refundType = refundType;
        this.refundStatus = refundStatus;
        this.occurredAtText = formatOccurredAt(occurredAt);
        this.idempotencyKey = idempotencyKey;
    }

    @JsonIgnore
    public LocalDateTime getOccurredAt() {
        if (occurredAtText == null || occurredAtText.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(occurredAtText, OCCURRED_AT_FORMATTER);
    }

    @JsonIgnore
    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAtText = formatOccurredAt(occurredAt);
    }

    private static String formatOccurredAt(LocalDateTime occurredAt) {
        if (occurredAt == null) {
            return null;
        }
        return occurredAt.format(OCCURRED_AT_FORMATTER);
    }
}
