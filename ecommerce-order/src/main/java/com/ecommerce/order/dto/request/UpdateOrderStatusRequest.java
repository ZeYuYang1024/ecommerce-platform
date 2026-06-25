package com.ecommerce.order.dto.request;

import com.ecommerce.common.constant.OrderStatus;
import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Data
public class UpdateOrderStatusRequest {
    @NotNull(message = "status is required")
    private Integer status;

    @AssertTrue(message = "status can only be 0, 1, or 4; shipping and completion are driven by logistics events")
    public boolean isManualStatusAllowed() {
        return status == null
                || status == OrderStatus.PENDING
                || status == OrderStatus.PAID
                || status == OrderStatus.CANCELLED;
    }
}
