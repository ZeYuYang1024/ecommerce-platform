package com.ecommerce.common.dto;

import java.io.Serializable;

public class OrderItemMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long skuId;
    private Integer quantity;

    public OrderItemMessage() {}

    public OrderItemMessage(Long skuId, Integer quantity) {
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
