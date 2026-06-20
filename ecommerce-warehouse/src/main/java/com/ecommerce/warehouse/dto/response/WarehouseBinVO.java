package com.ecommerce.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseBinVO {
    private Long id;
    private Long zoneId;
    private Long warehouseId;
    private String binCode;
    private String binType;
    private String binTypeText;
}
