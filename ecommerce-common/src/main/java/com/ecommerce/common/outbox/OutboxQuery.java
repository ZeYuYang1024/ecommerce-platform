package com.ecommerce.common.outbox;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxQuery {
    private String aggregateType;
    private String topic;
    private Integer status;
    private String aggregateId;
}
