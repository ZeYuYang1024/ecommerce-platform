package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockOperateRequest {
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @Min(value = 1, message = "数量必须大于0")
    private int quantity;

    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
