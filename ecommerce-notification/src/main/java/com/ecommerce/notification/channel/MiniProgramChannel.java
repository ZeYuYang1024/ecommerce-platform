package com.ecommerce.notification.channel;

import com.ecommerce.notification.entity.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MiniProgramChannel implements NotificationChannel {
    @Override public String getType() { return "MINI_PROGRAM"; }

    @Override
    public void send(NotificationLog logEntry) {
        log.info("[MINI_PROGRAM] TO:{} | {} — {}", logEntry.getRecipient(), logEntry.getTitle(), logEntry.getContent());
    }
}
