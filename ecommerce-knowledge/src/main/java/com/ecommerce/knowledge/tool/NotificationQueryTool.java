package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.NotificationClient;
import com.ecommerce.knowledge.client.dto.NotificationVO;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationQueryTool {

    private final NotificationClient notificationClient;

    @Tool("查询当前登录用户最近收到的通知消息，适用于发货、支付、取消等通知查询。")
    public List<NotificationVO> queryCurrentUserNotifications() {
        Long userId = currentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            var result = notificationClient.getCurrentUserNotifications(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception e) {
            log.warn("Failed to query notifications for user {}", userId, e);
        }
        return Collections.emptyList();
    }

    private Long currentUserId() {
        var context = AgentUserContextHolder.get();
        return context != null ? context.userId() : null;
    }
}
