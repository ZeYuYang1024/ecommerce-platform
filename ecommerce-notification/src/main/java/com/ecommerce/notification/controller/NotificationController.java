package com.ecommerce.notification.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.notification.entity.NotificationLog;
import com.ecommerce.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public Result<List<NotificationLog>> myNotifications(@RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return Result.ok(notificationService.listLogs(userId));
    }
}
