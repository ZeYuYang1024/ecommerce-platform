package com.ecommerce.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseZoneVO {
    private Long id;
    private Long warehouseId;
    private String zoneName;
    private String zoneCode;
    private String zoneType;
    private String zoneTypeText;
}
