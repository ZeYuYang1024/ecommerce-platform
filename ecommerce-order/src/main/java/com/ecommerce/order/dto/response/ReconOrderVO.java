package com.ecommerce.order.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReconOrderVO {
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
}
