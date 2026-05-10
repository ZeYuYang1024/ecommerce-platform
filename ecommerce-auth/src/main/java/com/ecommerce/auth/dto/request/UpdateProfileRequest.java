package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 20, message = "手机号最长20位")
    private String phone;

    @Size(max = 512, message = "头像URL最长512位")
    private String avatar;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
