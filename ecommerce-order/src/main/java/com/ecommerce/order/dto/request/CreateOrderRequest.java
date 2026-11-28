package com.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotEmpty
    private List<@NotNull @Valid OrderItemRequest> items;
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
        @NotNull
        @Positive
        private Long skuId;
        private Long spuId;
        private String name;
        private String image;
        private String price;
        @NotNull
        @Positive
        private Integer quantity;
    }
}
