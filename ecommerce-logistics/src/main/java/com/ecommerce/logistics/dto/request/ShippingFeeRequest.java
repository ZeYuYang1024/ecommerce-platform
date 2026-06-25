package com.ecommerce.logistics.dto.request;

import lombok.Data;

@Data
public class ShippingFeeRequest {
    private Long templateId;
    private Integer quantity = 0;
    private Integer weight = 0;
    private Integer volume = 0;
    private String provinceCode;
}
