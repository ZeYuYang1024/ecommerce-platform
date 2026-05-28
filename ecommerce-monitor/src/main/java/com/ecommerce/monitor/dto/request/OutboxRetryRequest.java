package com.ecommerce.monitor.dto.request;

import lombok.Data;

@Data
public class OutboxRetryRequest {

    private Long messageId;
    private Integer status;
    private String topic;
    private String aggregateId;
    private Integer limit;
}
