package com.ecommerce.merchant.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantAuditRequest {
    @NotNull(message = "审核动作不能为空")
    private Integer action;
    private String comment;
}
