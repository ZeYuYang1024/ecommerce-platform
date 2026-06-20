package com.ecommerce.order.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private Long id;
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private BigDecimal originalAmount;
    private Integer pointsUsed;
    private BigDecimal pointsDeductionAmount;
    private Integer pointsDeductionRatio;
    private Integer status;
    private String statusText;
    private String fulfillmentStatus;
    private String fulfillmentStatusText;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private List<OrderItemVO> items;
    private LocalDateTime createdAt;

    @Data
    public static class OrderItemVO {
        private Long id;
        private Long skuId;
        private Long spuId;
        private String name;
        private String image;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalPrice;
    }
}
