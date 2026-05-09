package com.ecommerce.order.client;

public class StockOperateRequest {
    private Long skuId;
    private Integer quantity;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
