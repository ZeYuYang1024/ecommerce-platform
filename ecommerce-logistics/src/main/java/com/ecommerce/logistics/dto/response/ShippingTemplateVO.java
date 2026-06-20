package com.ecommerce.logistics.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShippingTemplateVO {
    private Long id;
    private String templateName;
    private Long merchantId;
    private Integer calcType;
    private String calcTypeText;
    private Integer firstUnit;
    private BigDecimal firstFee;
    private Integer continueUnit;
    private BigDecimal continueFee;
    private String freeCondition;
    private String regionRules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
