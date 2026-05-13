package com.ecommerce.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.CouponVerifyVO;
import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.entity.CouponTemplate;
import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    // 模板管理（admin）
    CouponTemplate createTemplate(CouponTemplate template);
    CouponTemplate updateTemplate(CouponTemplate template);
    Page<CouponTemplate> listTemplates(Integer status, int page, int size);

    // 用户领券
    void claim(Long userId, Long templateId);

    // 可领取优惠券列表（公开）
    List<CouponVO> listAvailableCoupons();
    Page<CouponVO> listAvailableCoupons(int page, int size);

    // 用户券列表
    List<CouponVO> listUserCoupons(Long userId, Integer status);
    Page<CouponVO> listUserCoupons(Long userId, Integer status, int page, int size);

    // 下单校验（内部调用）
    CouponVerifyVO verify(Long userCouponId, Long userId, BigDecimal orderAmount);

    // 核销（下单后调用）
    void use(Long userCouponId, String orderNo);
}
