package com.ecommerce.auth.dto.response;

public class MerchantAccountVO {
    private String username;
    private boolean created;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public boolean isCreated() { return created; }
    public void setCreated(boolean created) { this.created = created; }
}
