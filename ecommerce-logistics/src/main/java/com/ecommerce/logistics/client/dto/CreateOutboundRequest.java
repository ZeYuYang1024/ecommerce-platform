package com.ecommerce.logistics.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateOutboundRequest {
    private Long warehouseId;
    private String outboundType;
    private Long shippingId;
    private Long merchantId;
    private List<OutboundItemRequest> items;

    @Data
    public static class OutboundItemRequest {
        private Long skuId;
        private Integer quantity;
        private Long binId;
    }
}
