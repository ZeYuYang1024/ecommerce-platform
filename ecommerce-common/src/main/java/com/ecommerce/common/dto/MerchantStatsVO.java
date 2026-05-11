package com.ecommerce.common.dto;

import lombok.Data;

@Data
public class MerchantStatsVO {
    private long merchantCount;
    private long pendingAuditCount;
}
