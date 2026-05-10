package com.ecommerce.auth.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.common.dto.MerchantStatsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "ecommerce-merchant")
public interface MerchantStatsClient {

    @GetMapping("/api/v1/admin/merchants/stats")
    Result<MerchantStatsVO> stats();
}
