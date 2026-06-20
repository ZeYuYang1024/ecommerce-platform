package com.ecommerce.logistics.client.dto;

import lombok.Data;

import java.util.List;

@Data
public class OutboundOrderVO {
    private Long id;
    private String outboundNo;
    private Long warehouseId;
    private Long shippingId;
    private Integer status;
    private String statusText;
    private List<OutboundItem> items;

    @Data
    public static class OutboundItem {
        private Long skuId;
        private Integer quantity;
    }
}
