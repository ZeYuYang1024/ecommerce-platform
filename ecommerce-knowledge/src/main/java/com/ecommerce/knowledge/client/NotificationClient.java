package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.NotificationVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ecommerce-notification", path = "/api/v1")
public interface NotificationClient {

    @GetMapping("/notifications")
    Result<List<NotificationVO>> getCurrentUserNotifications(@RequestHeader("X-User-Id") Long userId);
}
