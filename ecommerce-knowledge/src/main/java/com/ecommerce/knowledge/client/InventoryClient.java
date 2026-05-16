package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.InventoryVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ecommerce-inventory", path = "/api/v1/inventory")
public interface InventoryClient {

    @GetMapping("/{skuId}")
    Result<InventoryVO> getBySkuId(@PathVariable Long skuId);
}
