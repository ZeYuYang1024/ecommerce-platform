package com.ecommerce.common.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class ReconOrderVO {
    private String orderNo;
    private BigDecimal amount;
    private Integer status;
}
