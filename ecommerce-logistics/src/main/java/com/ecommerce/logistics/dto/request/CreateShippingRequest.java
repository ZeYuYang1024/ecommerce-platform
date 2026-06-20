package com.ecommerce.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class CreateShippingRequest {
    @NotBlank(message = "幂等键不能为空")
    private String clientRequestId;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "物流公司ID不能为空")
    private Long providerId;

    @NotBlank(message = "运单号不能为空")
    private String trackingNo;

    private Long warehouseId;
    private String senderInfo;
    private String receiverInfo;
    private Integer packageWeight;
    private String packageSize;
    private Integer sourceType;

    @NotEmpty(message = "发货明细不能为空")
    private List<ShippingItemRequest> items;

    @Data
    public static class ShippingItemRequest {
        @NotNull private Long orderItemId;
        @NotNull private Long skuId;
        @NotNull private Integer quantity;
    }
}
