package com.ecommerce.common.dto;

import java.io.Serializable;

public class MerchantApprovedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long merchantId;
    private String merchantName;

    public MerchantApprovedMessage() {}

    public MerchantApprovedMessage(Long merchantId, String merchantName) {
        this.merchantId = merchantId;
        this.merchantName = merchantName;
    }

    public Long getMerchantId() { return merchantId; }
    public void setMerchantId(Long merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
}
