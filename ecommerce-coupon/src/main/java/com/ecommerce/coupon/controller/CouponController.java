package com.ecommerce.coupon.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/coupons/claim")
    public Result<String> claim(@RequestParam Long templateId,
                                 @RequestHeader("X-User-Id") Long userId) {
        couponService.claim(userId, templateId);
        return Result.ok("领取成功");
    }

    @GetMapping("/coupons")
    public Result<List<CouponVO>> myCoupons(@RequestParam(required = false) Integer status,
                                             @RequestHeader("X-User-Id") Long userId) {
        return Result.ok(couponService.listUserCoupons(userId, status));
    }
}
