package com.ecommerce.notification.service;

import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationTemplate;
import java.util.List;
import java.util.Map;

public interface NotificationService {
    // 模板管理
    NotificationTemplate createTemplate(NotificationTemplate template);
    List<NotificationTemplate> listTemplates();

    // 发送通知（按模板编码 + 参数）
    NotificationLog send(String templateCode, Long userId, Map<String, String> params);

    // 用户通知历史
    List<NotificationLog> listLogs(Long userId);
}
