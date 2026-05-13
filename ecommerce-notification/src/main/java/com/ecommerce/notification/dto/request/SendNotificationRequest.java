package com.ecommerce.notification.dto.request;

import lombok.Data;
import java.util.Map;

@Data
public class SendNotificationRequest {
    private String recipient;
    private Map<String, String> params;
}
