package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StockSetRequest {
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer totalStock;

    public Integer getTotalStock() { return totalStock; }
    public void setTotalStock(Integer totalStock) { this.totalStock = totalStock; }
}
