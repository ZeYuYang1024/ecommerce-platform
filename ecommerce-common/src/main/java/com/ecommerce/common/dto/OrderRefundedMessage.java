package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String refundNo;
    private String orderNo;
    private Long userId;
    private BigDecimal refundAmount;
    private String refundType;
    private String refundStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
    private String idempotencyKey;
}
