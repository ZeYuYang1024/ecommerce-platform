package com.ecommerce.logistics.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BatchShipRequest {
    private List<BatchShipItem> items;

    @Data
    public static class BatchShipItem {
        private Long orderId;
        private Long providerId;
        private String trackingNo;
        private Integer packageWeight;
    }
}
