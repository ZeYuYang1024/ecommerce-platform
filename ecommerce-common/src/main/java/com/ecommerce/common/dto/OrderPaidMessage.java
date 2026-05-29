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
public class OrderPaidMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;
    private String transactionId;
    private String idempotencyKey;
    private String errorMessage;

    public OrderPaidMessage(String orderNo, Integer status, LocalDateTime paidAt) {
        this(orderNo, status, paidAt, null, null, null);
    }
}
