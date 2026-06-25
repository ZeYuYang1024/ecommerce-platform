package com.ecommerce.logistics.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateShippingTemplateRequest {
    private String templateName;
    private Long merchantId;
    private Integer calcType;
    private Integer firstUnit;
    private BigDecimal firstFee;
    private Integer continueUnit;
    private BigDecimal continueFee;
    private String freeCondition;
    private String regionRules;
}
