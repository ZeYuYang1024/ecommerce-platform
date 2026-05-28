package com.ecommerce.monitor.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReliabilityOverviewVO {

    private long failedOrderOutboxCount;
    private long failedPaymentOutboxCount;
    private long exhaustedRetryCount;
    private long inventoryProcessedCount;
    private long inventoryProcessingCount;
    private long orderPendingCount;
    private long paymentPendingCount;
    private Long oldestRetryableAgeMinutes;
    private List<ReliabilityWarningVO> warnings = new ArrayList<>();
    private List<String> degradedSections = new ArrayList<>();
}
