package com.ecommerce.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingExceptionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long shippingId;
    private Long orderId;
    private String orderNo;
    private String trackingNo;
    private String exceptionDesc;
    private String transactionId;
    private String idempotencyKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime occurredAt;
}
