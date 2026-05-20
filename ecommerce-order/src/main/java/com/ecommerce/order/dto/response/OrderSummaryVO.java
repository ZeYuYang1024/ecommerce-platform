package com.ecommerce.order.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSummaryVO {
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private String statusText;
    private String itemSummary;
    private String firstItemName;
    private Integer itemCount;
    private LocalDateTime createdAt;
}
