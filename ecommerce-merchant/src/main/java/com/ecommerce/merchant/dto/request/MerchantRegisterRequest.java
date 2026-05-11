package com.ecommerce.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MerchantRegisterRequest {
    @NotBlank(message = "店铺名称不能为空")
    private String name;
    private String logo;
    @NotBlank(message = "联系人不能为空")
    private String contactName;
    @NotBlank(message = "联系电话不能为空")
    private String contactPhone;
    @NotBlank(message = "营业执照不能为空")
    private String businessLicense;
}
