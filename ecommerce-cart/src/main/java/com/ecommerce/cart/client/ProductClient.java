package com.ecommerce.cart.client;

import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ecommerce-product", fallbackFactory = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/api/v1/internal/products/skus/batch")
    Result<List<SkuBatchVO>> getSkus(@RequestParam("ids") List<Long> ids);
}
