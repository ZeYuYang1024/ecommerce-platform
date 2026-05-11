package com.ecommerce.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardStatsVO {
    private long userCount;
    private long merchantCount;
    private long pendingAuditCount;
    private long productCount;
}
