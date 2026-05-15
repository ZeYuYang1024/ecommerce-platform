package com.ecommerce.knowledge.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponVO {
    private Long id;
    private String name;
    private String type;
    private String typeDesc;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalCount;
    private Integer receivedCount;
    private Integer remainCount;
}
