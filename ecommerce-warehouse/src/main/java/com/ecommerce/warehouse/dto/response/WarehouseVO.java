package com.ecommerce.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseVO {
    private Long id;
    private String warehouseName;
    private String warehouseCode;
    private Integer warehouseType;
    private String warehouseTypeText;
    private Integer stockMode;
    private String stockModeText;
    private Long merchantId;
    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactPhone;
    private Integer status;
    private String statusText;
}
