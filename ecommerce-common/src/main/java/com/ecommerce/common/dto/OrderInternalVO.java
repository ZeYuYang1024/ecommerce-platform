package com.ecommerce.common.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderInternalVO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
}
