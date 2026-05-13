package com.ecommerce.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/coupons/claim")
    public Result<String> claim(@RequestParam Long templateId,
                                 @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId == null) return Result.fail(401, "请先登录");
        couponService.claim(userId, templateId);
        return Result.ok("领取成功");
    }

    @GetMapping("/coupons")
    public Result<Page<CouponVO>> myCoupons(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        if (userId != null) {
            return Result.ok(couponService.listUserCoupons(userId, status, page, size));
        }
        return Result.ok(couponService.listAvailableCoupons(page, size));
    }
}
