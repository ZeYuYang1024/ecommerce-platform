package com.ecommerce.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;

    @NotNull(message = "仓库类型不能为空")
    private Integer warehouseType;

    @NotNull(message = "库存管理模式不能为空")
    private Integer stockMode;

    private Long merchantId;

    private String province;
    private String city;
    private String district;
    private String address;
    private String contactName;
    private String contactPhone;
}
