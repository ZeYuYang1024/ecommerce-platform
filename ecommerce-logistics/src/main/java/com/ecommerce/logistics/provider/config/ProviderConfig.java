package com.ecommerce.logistics.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "logistics.provider")
public class ProviderConfig {
    private String active = "stub";

    public String getActive() { return active; }
    public void setActive(String active) { this.active = active; }
}
