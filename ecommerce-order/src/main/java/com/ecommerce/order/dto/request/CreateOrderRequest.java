package com.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty
    private List<OrderItemRequest> items;
    @NotBlank
    private String receiverName;
    @NotBlank
    private String receiverPhone;
    @NotBlank
    private String receiverAddress;
    private Boolean usePoints;
    private Integer pointsToUse;
    private String clientRequestId;

    @Data
    public static class OrderItemRequest {
        private Long skuId;
        private Long spuId;
        private String name;
        private String image;
        private String price;
        private Integer quantity;
    }
}
