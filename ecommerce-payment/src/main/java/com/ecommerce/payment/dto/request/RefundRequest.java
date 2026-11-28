package com.ecommerce.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotBlank
    @Size(max = 256)
    private String reason;

    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;

    @Pattern(regexp = "[A-Za-z0-9_-]{1,64}")
    private String clientRequestId;
}
