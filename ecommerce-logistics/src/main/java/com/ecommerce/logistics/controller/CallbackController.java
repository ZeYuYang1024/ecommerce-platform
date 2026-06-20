package com.ecommerce.logistics.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.logistics.service.ShippingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;

@Slf4j
@RestController
@RequestMapping("/api/v1/callback/logistics")
@RequiredArgsConstructor
public class CallbackController {

    private final ShippingService shippingService;

    @PostMapping("/{providerCode}")
    public Result<String> callback(@PathVariable String providerCode, HttpServletRequest request) {
        try {
            StringBuilder body = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            String rawBody = body.toString();
            String signature = request.getHeader("X-Signature");
            shippingService.processCallback(providerCode, rawBody, signature);
            return Result.ok("success");
        } catch (Exception e) {
            log.error("Callback processing error: provider={}", providerCode, e);
            return Result.ok("success"); // 始终返回 success 防止第三方重试
        }
    }
}
