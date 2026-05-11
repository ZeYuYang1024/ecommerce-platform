package com.ecommerce.merchant.dto.response;

import lombok.Data;

@Data
public class MerchantStatsVO {
    private long merchantCount;
    private long pendingAuditCount;
}
