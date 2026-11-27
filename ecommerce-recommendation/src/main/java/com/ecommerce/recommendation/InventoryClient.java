package com.ecommerce.recommendation;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ecommerce-inventory")
interface InventoryClient {
    @PostMapping("/api/v1/inventory/batch-query")
    Result<List<Stock>> stock(@RequestBody List<Long> ids);

    record Stock(Long skuId, Integer availableStock) {}
}
