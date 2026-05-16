package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.NotificationClient;
import com.ecommerce.knowledge.client.dto.NotificationVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryToolTest {

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private NotificationQueryTool notificationQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserNotificationsReturnsNotificationsForCurrentUser() {
        AgentUserContextHolder.set(new AgentUserContext(1003L, "USER"));
        NotificationVO notification = new NotificationVO();
        notification.setTitle("Order shipped");
        when(notificationClient.getCurrentUserNotifications(1003L)).thenReturn(Result.ok(List.of(notification)));

        List<NotificationVO> result = notificationQueryTool.queryCurrentUserNotifications();

        assertEquals(1, result.size());
        assertEquals("Order shipped", result.getFirst().getTitle());
        verify(notificationClient).getCurrentUserNotifications(1003L);
    }

    @Test
    void queryCurrentUserNotificationsReturnsEmptyListWhenCurrentUserMissing() {
        List<NotificationVO> result = notificationQueryTool.queryCurrentUserNotifications();

        assertTrue(result.isEmpty());
        verify(notificationClient, never()).getCurrentUserNotifications(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void queryCurrentUserNotificationsReturnsEmptyListWhenClientThrows() {
        AgentUserContextHolder.set(new AgentUserContext(1003L, "USER"));
        when(notificationClient.getCurrentUserNotifications(1003L)).thenThrow(new RuntimeException("boom"));

        List<NotificationVO> result = notificationQueryTool.queryCurrentUserNotifications();

        assertTrue(result.isEmpty());
    }
}
