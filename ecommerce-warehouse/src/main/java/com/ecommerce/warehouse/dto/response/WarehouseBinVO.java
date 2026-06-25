package com.ecommerce.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseBinVO {
    private Long id;
    private Long zoneId;
    private Long warehouseId;
    private String binCode;
    private Integer binType;
    private String binTypeText;
}
