package com.ecommerce.inventory.client;

import com.ecommerce.common.dto.SkuBatchVO;
import com.ecommerce.common.dto.SkuOwnerVO;
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

    @GetMapping("/api/v1/internal/sku-ids")
    Result<List<Long>> listSkuIdsByMerchant(@RequestParam("merchantId") Long merchantId);

    @GetMapping("/api/v1/internal/sku-owner")
    Result<SkuOwnerVO> querySkuOwner(@RequestParam("skuId") Long skuId);

    @Component
    static class ProductClientFallback implements FallbackFactory<ProductClient> {
        @Override
        public ProductClient create(Throwable cause) {
            return new ProductClient() {
                @Override
                public Result<List<SkuBatchVO>> batchQuerySkus(List<Long> ids) {
                    return Result.ok(List.of());
                }

                @Override
                public Result<List<Long>> listSkuIdsByMerchant(Long merchantId) {
                    return Result.ok(List.of());
                }

                @Override
                public Result<SkuOwnerVO> querySkuOwner(Long skuId) {
                    return Result.ok(null);
                }
            };
        }
    }
}
