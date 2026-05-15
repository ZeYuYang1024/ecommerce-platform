package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.InventoryVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ecommerce-inventory", path = "/api/v1")
public interface InventoryClient {

    @GetMapping("/inventory")
    Result<List<InventoryVO>> query(@RequestParam(required = false) String skuCode,
                                     @RequestParam(required = false) String skuName,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size);
}
