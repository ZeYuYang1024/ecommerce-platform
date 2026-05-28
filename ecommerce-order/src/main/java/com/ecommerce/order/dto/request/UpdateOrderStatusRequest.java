package com.ecommerce.order.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateOrderStatusRequest {
    @NotNull(message = "status is required")
    @Min(value = 0, message = "status must be between 0 and 5")
    @Max(value = 5, message = "status must be between 0 and 5")
    private Integer status;
}
