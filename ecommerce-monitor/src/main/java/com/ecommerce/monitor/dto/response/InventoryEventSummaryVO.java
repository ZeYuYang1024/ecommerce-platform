package com.ecommerce.monitor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEventSummaryVO {

    private int processingCount;
    private int processedCount;
    private int failedCount;

    public InventoryEventSummaryVO(int processingCount, int processedCount) {
        this(processingCount, processedCount, 0);
    }
}
