package com.ecommerce.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsVO {
    private long userCount;
    private long merchantCount;
    private long pendingAuditCount;
    private long productCount;

    public long getUserCount() { return userCount; }
    public void setUserCount(long userCount) { this.userCount = userCount; }
    public long getMerchantCount() { return merchantCount; }
    public void setMerchantCount(long merchantCount) { this.merchantCount = merchantCount; }
    public long getPendingAuditCount() { return pendingAuditCount; }
    public void setPendingAuditCount(long pendingAuditCount) { this.pendingAuditCount = pendingAuditCount; }
    public long getProductCount() { return productCount; }
    public void setProductCount(long productCount) { this.productCount = productCount; }
}
