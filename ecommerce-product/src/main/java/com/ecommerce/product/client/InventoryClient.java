package com.ecommerce.product.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-inventory")
public interface InventoryClient {

    @PostMapping("/api/v1/inventory/admin/{skuId}")
    Result<Void> initStock(@PathVariable Long skuId, @RequestBody StockSetRequest request);
}
