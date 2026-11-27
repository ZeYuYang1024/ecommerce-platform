package com.ecommerce.recommendation;

import com.ecommerce.common.dto.RecommendationProduct;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "ecommerce-product")
interface ProductClient {
    @PostMapping("/api/v1/internal/products/recommendation-candidates")
    Result<List<RecommendationProduct>> candidates(@RequestBody RecommendationProduct.Query query);
}
