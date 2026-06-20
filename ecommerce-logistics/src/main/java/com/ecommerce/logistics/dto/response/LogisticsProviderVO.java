package com.ecommerce.logistics.dto.response;

import lombok.Data;

@Data
public class LogisticsProviderVO {
    private Long id;
    private String providerCode;
    private String providerName;
    private String providerLogo;
    private Integer supportWaybill;
    private Integer status;
    private String statusText;
    private Integer priority;
}
