package com.ecommerce.common.dto;

import java.math.BigDecimal;

public record ManualRefundCommand(String requestId, String reason, BigDecimal amount, boolean afterShipment) {
}
