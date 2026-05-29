package com.ecommerce.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryEventSummaryVO {

    private int processingCount;
    private int processedCount;
    private int failedCount;

    public InventoryEventSummaryVO(int processingCount, int processedCount) {
        this(processingCount, processedCount, 0);
    }
}
