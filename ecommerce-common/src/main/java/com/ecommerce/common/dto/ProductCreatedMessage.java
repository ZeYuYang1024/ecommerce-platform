package com.ecommerce.common.dto;

import java.io.Serializable;

public class ProductCreatedMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long spuId;
    private Long skuId;

    public ProductCreatedMessage() {}

    public ProductCreatedMessage(Long spuId, Long skuId) {
        this.spuId = spuId;
        this.skuId = skuId;
    }

    public Long getSpuId() { return spuId; }
    public void setSpuId(Long spuId) { this.spuId = spuId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
}
