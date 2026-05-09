package com.ecommerce.merchant.dto.request;

import jakarta.validation.constraints.NotBlank;

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getBusinessLicense() { return businessLicense; }
    public void setBusinessLicense(String businessLicense) { this.businessLicense = businessLicense; }
}
