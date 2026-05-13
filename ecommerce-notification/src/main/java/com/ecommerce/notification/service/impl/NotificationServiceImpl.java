package com.ecommerce.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.notification.channel.NotificationChannel;
import com.ecommerce.notification.common.NotificationErrorCode;
import com.ecommerce.notification.dto.request.SendNotificationRequest;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationTemplate;
import com.ecommerce.notification.mapper.NotificationLogMapper;
import com.ecommerce.notification.mapper.NotificationTemplateMapper;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationTemplateMapper templateMapper;
    private final NotificationLogMapper logMapper;
    private final List<NotificationChannel> channels;

    @Override
    public NotificationTemplate createTemplate(NotificationTemplate template) {
        templateMapper.insert(template);
        return template;
    }

    @Override
    public List<NotificationTemplate> listTemplates() {
        return templateMapper.selectList(new LambdaQueryWrapper<NotificationTemplate>()
                .orderByDesc(NotificationTemplate::getCreatedAt));
    }

    @Override
    @Transactional
    public NotificationLog send(String templateCode, Long userId, SendNotificationRequest request) {
        NotificationTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<NotificationTemplate>()
                        .eq(NotificationTemplate::getTemplateCode, templateCode)
                        .eq(NotificationTemplate::getStatus, 1)
                        .last("LIMIT 1"));
        if (template == null) {
            log.warn("Template not found or disabled: {}", templateCode);
            return null;
        }

        Map<String, String> params = request.getParams() != null ? request.getParams() : java.util.Collections.emptyMap();

        String content = template.getContent() != null ? template.getContent() : "";
        for (Map.Entry<String, String> e : params.entrySet()) {
            content = content.replace("{" + e.getKey() + "}", e.getValue());
        }
        String title = template.getTitle() != null ? template.getTitle() : "";
        for (Map.Entry<String, String> e : params.entrySet()) {
            title = title.replace("{" + e.getKey() + "}", e.getValue());
        }

        NotificationLog logEntry = new NotificationLog();
        logEntry.setUserId(userId);
        logEntry.setTemplateId(template.getId());
        logEntry.setType(template.getType());
        logEntry.setTitle(title);
        logEntry.setContent(content);
        logEntry.setStatus(0);
        logEntry.setRecipient(request.getRecipient());
        logMapper.insert(logEntry);

        // 发送
        for (NotificationChannel channel : channels) {
            if (channel.getType().equals(template.getType())) {
                try {
                    channel.send(logEntry);
                    logEntry.setStatus(1);
                    logEntry.setSentAt(LocalDateTime.now());
                } catch (Exception e) {
                    log.error("Notification send failed: id={}", logEntry.getId(), e);
                    logEntry.setStatus(2);
                    logEntry.setErrorMsg(e.getMessage());
                }
                logMapper.updateById(logEntry);
                return logEntry;
            }
        }
        logEntry.setStatus(2);
        logEntry.setErrorMsg("No matching channel: " + template.getType());
        logMapper.updateById(logEntry);
        return logEntry;
    }

    @Override
    public List<NotificationLog> listLogs(Long userId) {
        return logMapper.selectList(new LambdaQueryWrapper<NotificationLog>()
                .eq(NotificationLog::getUserId, userId)
                .orderByDesc(NotificationLog::getCreatedAt));
    }
}
