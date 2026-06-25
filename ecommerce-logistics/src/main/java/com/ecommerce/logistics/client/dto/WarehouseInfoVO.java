package com.ecommerce.logistics.client.dto;

import lombok.Data;

@Data
public class WarehouseInfoVO {
    private Long id;
    private String warehouseName;
    private String warehouseCode;
    private Integer warehouseType;
    private String warehouseTypeText;
    private Integer stockMode;
    private String stockModeText;
    private Long merchantId;
    private Integer status;
    private String statusText;
}
