package com.ecommerce.logistics.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingFeeResponse {
    private BigDecimal shippingFee;
}
