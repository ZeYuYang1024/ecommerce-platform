package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public class CreateMerchantAccountRequest {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    private String merchantName;

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
}
