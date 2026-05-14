package com.ecommerce.inventory.client;

import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ecommerce-product", fallbackFactory = ProductClient.ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/api/v1/products/skus/batch")
    Result<List<SkuBatchVO>> batchQuerySkus(@RequestParam("ids") List<Long> ids);

    @Component
    static class ProductClientFallback implements FallbackFactory<ProductClient> {
        @Override
        public ProductClient create(Throwable cause) {
            return ids -> Result.ok(List.of());
        }
    }
}
