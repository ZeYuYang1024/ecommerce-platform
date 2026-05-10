package com.ecommerce.common.dto;

public class MerchantStatsVO {
    private long merchantCount;
    private long pendingAuditCount;

    public long getMerchantCount() { return merchantCount; }
    public void setMerchantCount(long merchantCount) { this.merchantCount = merchantCount; }
    public long getPendingAuditCount() { return pendingAuditCount; }
    public void setPendingAuditCount(long pendingAuditCount) { this.pendingAuditCount = pendingAuditCount; }
}
