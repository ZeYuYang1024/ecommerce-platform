package com.ecommerce.notification.channel;

import com.ecommerce.notification.entity.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsChannel implements NotificationChannel {
    @Override public String getType() { return "SMS"; }

    @Override
    public void send(NotificationLog logEntry) {
        log.info("[SMS] TO:{} | {} — {}", logEntry.getRecipient(), logEntry.getTitle(), logEntry.getContent());
    }
}
