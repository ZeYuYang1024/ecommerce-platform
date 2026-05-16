package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private Long userCouponId;
    private String name;
    private String type;
    private BigDecimal minAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
