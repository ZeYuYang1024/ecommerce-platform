package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ecommerce-product")
public interface ProductSpuClient {

    @GetMapping("/api/v1/internal/spu-ids")
    Result<List<Long>> getSpuIdsByMerchant(@RequestParam("merchantId") Long merchantId);
}
