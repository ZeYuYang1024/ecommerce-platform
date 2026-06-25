package com.ecommerce.logistics.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * NOTE: This class is duplicated between ecommerce-logistics (source of truth)
 * and ecommerce-order (Feign client DTO). When adding fields, update BOTH copies.
 */
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
