package com.ecommerce.notification.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.notification.dto.request.SendNotificationRequest;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.entity.NotificationTemplate;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notification/templates")
    public Result<List<NotificationTemplate>> listTemplates() {
        return Result.ok(notificationService.listTemplates());
    }

    @PostMapping("/notification/templates")
    public Result<NotificationTemplate> createTemplate(@RequestBody NotificationTemplate template) {
        return Result.ok(notificationService.createTemplate(template));
    }

    @PostMapping("/notification/send")
    public Result<NotificationLog> send(@RequestParam String templateCode,
                                         @RequestBody SendNotificationRequest request) {
        return Result.ok(notificationService.send(templateCode, null, request));
    }
}
