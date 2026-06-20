package com.ecommerce.logistics.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FulfillmentSummaryVO {
    private Long orderId;
    private String fulfillmentStatus;
    private String fulfillmentStatusText;
    private String latestTraceDesc;
    private LocalDateTime latestTraceTime;
    private int shippingCount;
    private int deliveredCount;
}
