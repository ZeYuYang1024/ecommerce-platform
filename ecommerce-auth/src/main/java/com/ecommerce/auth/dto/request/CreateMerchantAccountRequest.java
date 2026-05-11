package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateMerchantAccountRequest {
    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    private String merchantName;
}
