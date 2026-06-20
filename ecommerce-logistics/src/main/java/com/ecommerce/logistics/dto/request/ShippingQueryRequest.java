package com.ecommerce.logistics.dto.request;

import lombok.Data;

@Data
public class ShippingQueryRequest {
    private String orderNo;
    private String trackingNo;
    private Integer shippingStatus;
    private Long merchantId;
}
