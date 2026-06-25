package com.ecommerce.warehouse.dto.request;

import lombok.Data;

@Data
public class UpdateWarehouseRequest {
    private String warehouseName;
    private Integer warehouseType;
    private Integer stockMode;
    private Long merchantId;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactPhone;
}
