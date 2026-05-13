package com.ecommerce.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.result.Result;
import com.ecommerce.coupon.entity.CouponTemplate;
import com.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping("/coupons")
    public Result<Page<CouponTemplate>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(couponService.listTemplates(status, page, size));
    }

    @GetMapping("/coupons/{id}")
    public Result<CouponTemplate> detail(@PathVariable Long id) {
        return Result.ok(null);
    }

    @PostMapping("/coupons")
    public Result<CouponTemplate> create(@RequestBody CouponTemplate template) {
        return Result.ok(couponService.createTemplate(template));
    }

    @PutMapping("/coupons/{id}")
    public Result<CouponTemplate> update(@PathVariable Long id, @RequestBody CouponTemplate template) {
        template.setId(id);
        return Result.ok(couponService.updateTemplate(template));
    }
}
