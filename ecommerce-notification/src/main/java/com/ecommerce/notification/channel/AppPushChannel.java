package com.ecommerce.notification.channel;

import com.ecommerce.notification.entity.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppPushChannel implements NotificationChannel {
    @Override public String getType() { return "APP_PUSH"; }

    @Override
    public void send(NotificationLog logEntry) {
        log.info("[APP_PUSH] TO:{} | {} — {}", logEntry.getRecipient(), logEntry.getTitle(), logEntry.getContent());
    }
}
