package com.ecommerce.product.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
