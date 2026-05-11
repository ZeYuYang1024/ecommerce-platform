package com.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockOperateRequest {
    @NotNull(message = "SKU ID 不能为空")
    private Long skuId;

    @Min(value = 1, message = "数量必须大于0")
    private int quantity;
}
