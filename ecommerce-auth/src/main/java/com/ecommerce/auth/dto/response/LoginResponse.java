package com.ecommerce.auth.dto.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String type;
    private Long merchantId;

    public static LoginResponse of(String token, Long userId, String username, String type) {
        return of(token, userId, username, type, null);
    }

    public static LoginResponse of(String token, Long userId, String username, String type, Long merchantId) {
        LoginResponse r = new LoginResponse();
        r.token = token;
        r.userId = userId;
        r.username = username;
        r.type = type;
        r.merchantId = merchantId;
        return r;
    }
}
