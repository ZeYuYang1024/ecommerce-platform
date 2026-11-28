package com.ecommerce.product.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-order", contextId = "reviewOrderClient")
public interface ReviewOrderClient {
    @GetMapping("/api/v1/internal/orders/{id}/review-eligible")
    Result<Boolean> reviewEligible(@PathVariable Long id, @RequestParam Long userId, @RequestParam Long spuId);
}
