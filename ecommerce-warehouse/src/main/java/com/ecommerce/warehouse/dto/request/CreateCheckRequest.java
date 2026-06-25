package com.ecommerce.warehouse.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCheckRequest {
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    private Long merchantId;
}
