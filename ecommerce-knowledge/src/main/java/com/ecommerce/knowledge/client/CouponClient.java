package com.ecommerce.knowledge.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.knowledge.client.dto.CouponVO;
import com.ecommerce.knowledge.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecommerce-coupon", path = "/api/v1")
public interface CouponClient {

    @GetMapping("/coupons")
    Result<Page<CouponVO>> listAvailable(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size);

    @GetMapping("/coupons")
    Result<Page<CouponVO>> listMine(@RequestHeader("X-User-Id") Long userId,
                                    @RequestParam(required = false) Integer status,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size);
}
