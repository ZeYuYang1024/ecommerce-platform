package com.ecommerce.auth.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String phone;
    private String avatar;
    private LocalDateTime createdAt;
}
