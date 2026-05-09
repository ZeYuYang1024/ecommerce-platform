package com.ecommerce.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class RefundRequest {
    @NotBlank
    private String reason;
    private BigDecimal amount;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
