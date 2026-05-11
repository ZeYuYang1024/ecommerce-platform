package com.ecommerce.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotBlank
    private String reason;
    private BigDecimal amount;
}
