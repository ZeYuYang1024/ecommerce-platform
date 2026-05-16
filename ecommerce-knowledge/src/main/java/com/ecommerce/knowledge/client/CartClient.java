package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.CartItemVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ecommerce-cart", path = "/api/v1")
public interface CartClient {

    @GetMapping("/cart")
    Result<List<CartItemVO>> getCurrentUserCart(@RequestHeader("X-User-Id") Long userId);

    @GetMapping("/cart/count")
    Result<Integer> getCurrentUserCartCount(@RequestHeader("X-User-Id") Long userId);
}
