package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class OutboxRetryRequest {

    private Long messageId;
    private Integer status;
    private String topic;
    private String aggregateId;

    @Min(1)
    private Integer limit;
}
