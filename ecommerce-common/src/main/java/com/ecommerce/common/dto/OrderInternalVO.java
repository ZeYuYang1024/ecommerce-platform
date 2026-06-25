package com.ecommerce.common.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderInternalVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private Integer pointsUsed;
    private BigDecimal pointsDeductionAmount;
    private Integer pointsDeductionRatio;
    private Integer status;
    private List<OrderItemSnapshot> items;

    @Data
    public static class OrderItemSnapshot {
        private Long orderItemId;
        private Long skuId;
        private Long merchantId;
        private Integer quantity;
    }
}
