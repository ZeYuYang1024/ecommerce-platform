package com.ecommerce.notification.channel;

import com.ecommerce.notification.entity.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailChannel implements NotificationChannel {
    @Override public String getType() { return "EMAIL"; }

    @Override
    public void send(NotificationLog logEntry) {
        log.info("[EMAIL] TO:{} | {} — {}", logEntry.getRecipient(), logEntry.getTitle(), logEntry.getContent());
    }
}
