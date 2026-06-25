package com.ecommerce.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateShippingTemplateRequest {
    @NotBlank(message = "模板名称不能为空")
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
