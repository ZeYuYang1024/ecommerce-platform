package com.ecommerce.payment.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SettlementVO {
    private Long id;
    private LocalDate settlementDate;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private Integer totalPaymentCount;
    private BigDecimal totalPaymentAmount;
    private Integer totalRefundCount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netAmount;
    private Integer status;
    private String statusText;
}
