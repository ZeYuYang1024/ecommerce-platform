package com.ecommerce.knowledge.client;

import com.ecommerce.knowledge.client.dto.CouponVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "ecommerce-coupon", path = "/api/v1")
public interface CouponClient {

    @GetMapping("/coupons")
    Result<List<CouponVO>> listAvailable();
}
