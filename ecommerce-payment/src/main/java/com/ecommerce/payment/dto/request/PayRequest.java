package com.ecommerce.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayRequest {
    @NotBlank
    private String orderNo;
    private Long orderId;
    @NotNull
    @PositiveOrZero
    private BigDecimal amount;
    private String payMethod = "wx_jsapi";
}
