package com.ecommerce.merchant.client;

import com.ecommerce.common.dto.CreateMerchantAccountRequest;
import com.ecommerce.common.dto.MerchantAccountVO;
import com.ecommerce.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-auth")
public interface AuthClient {

    @PostMapping("/api/v1/admin/merchant-account")
    Result<MerchantAccountVO> createMerchantAccount(@RequestBody CreateMerchantAccountRequest request);
}
