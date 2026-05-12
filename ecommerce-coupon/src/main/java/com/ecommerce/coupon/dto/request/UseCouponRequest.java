package com.ecommerce.coupon.dto.request;

import lombok.Data;

@Data
public class UseCouponRequest {
    private Long userCouponId;
    private String orderNo;
}
