package com.ecommerce.common.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderMemberVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private Integer pointsUsed;
    private BigDecimal pointsDeductionAmount;
    private Integer pointsDeductionRatio;
    private Integer status;
}
