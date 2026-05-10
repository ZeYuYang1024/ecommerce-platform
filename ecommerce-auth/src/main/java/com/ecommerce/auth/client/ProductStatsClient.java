package com.ecommerce.auth.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.common.dto.ProductStatsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ecommerce-product")
public interface ProductStatsClient {

    @GetMapping("/api/v1/admin/products/stats")
    Result<ProductStatsVO> stats();
}
