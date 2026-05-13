package com.ecommerce.common.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CouponVerifyVO {
    private boolean valid;
    private BigDecimal discount;
    private String couponName;
    private Long templateId;
}
