package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ecommerce-cart", fallbackFactory = CartClientFallback.class)
public interface CartClient {

    @GetMapping("/api/v1/cart")
    Result<List<Void>> getCart(@RequestHeader("X-User-Id") Long userId);
}
