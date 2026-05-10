package com.ecommerce.auth.dto.response;

public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
    private String type;

    private LoginResponse() {}

    public static LoginResponse of(String token, Long userId, String username, String type) {
        LoginResponse r = new LoginResponse();
        r.token = token;
        r.userId = userId;
        r.username = username;
        r.type = type;
        return r;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
