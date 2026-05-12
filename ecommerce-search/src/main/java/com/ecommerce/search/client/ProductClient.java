package com.ecommerce.search.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ecommerce-product")
public interface ProductClient {

    @GetMapping("/api/v1/products/{id}")
    Result<ProductDetailVO> getProductDetail(@PathVariable Long id);
}
