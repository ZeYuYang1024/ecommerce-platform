package com.ecommerce.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotBlank
    private String reason;

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    private BigDecimal amount;
}
