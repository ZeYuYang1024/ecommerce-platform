package com.ecommerce.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public class AddressRequest {
    @NotBlank(message = "收货人不能为空")
    private String receiverName;
    @NotBlank(message = "收货电话不能为空")
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    @NotBlank(message = "详细地址不能为空")
    private String detail;
    private Integer isDefault;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
}
