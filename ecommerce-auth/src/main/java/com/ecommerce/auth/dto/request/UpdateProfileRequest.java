package com.ecommerce.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 20, message = "手机号最长20位")
    private String phone;

    @Size(max = 512, message = "头像URL最长512位")
    private String avatar;
}
