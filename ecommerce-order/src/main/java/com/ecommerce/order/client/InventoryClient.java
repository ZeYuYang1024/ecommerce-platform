package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-inventory")
public interface InventoryClient {

    @PostMapping("/api/v1/inventory/deduct")
    Result<Void> deduct(@RequestBody StockOperateRequest request);

    @PostMapping("/api/v1/inventory/release")
    Result<Void> release(@RequestBody StockOperateRequest request);
}
