package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "ecommerce-cart")
public interface CartClient {

    @GetMapping("/api/v1/cart")
    Result<List<Map<String, Object>>> getCart(@RequestHeader("X-User-Id") Long userId);
}
