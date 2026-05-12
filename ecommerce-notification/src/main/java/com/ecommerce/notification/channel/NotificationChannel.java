package com.ecommerce.notification.channel;

import com.ecommerce.notification.entity.NotificationLog;

public interface NotificationChannel {
    void send(NotificationLog log);
    String getType();
}
