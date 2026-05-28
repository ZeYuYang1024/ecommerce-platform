package com.ecommerce.payment.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OutboxMessageVO {

    private Long id;
    private String aggregateId;
    private String topic;
    private Integer status;
    private Integer retryCount;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime createdAt;
}
