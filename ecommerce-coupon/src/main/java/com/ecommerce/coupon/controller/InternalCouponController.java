package com.ecommerce.coupon.controller;

import com.ecommerce.common.dto.CouponVerifyVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.coupon.dto.request.UseCouponRequest;
import com.ecommerce.coupon.dto.request.VerifyCouponRequest;
import com.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalCouponController {

    private final CouponService couponService;

    @PostMapping("/coupons/verify")
    public Result<CouponVerifyVO> verify(@RequestBody VerifyCouponRequest request) {
        return Result.ok(couponService.verify(
                request.getUserCouponId(), request.getUserId(), request.getOrderAmount()));
    }

    @PostMapping("/coupons/use")
    public Result<Void> use(@RequestBody UseCouponRequest request) {
        couponService.use(request.getUserCouponId(), request.getOrderNo());
        return Result.ok();
    }
}
