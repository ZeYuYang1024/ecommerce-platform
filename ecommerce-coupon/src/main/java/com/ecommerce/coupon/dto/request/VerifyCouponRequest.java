package com.ecommerce.coupon.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VerifyCouponRequest {
    private Long userCouponId;
    private Long userId;
    private BigDecimal orderAmount;
}
