package com.ecommerce.coupon.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.dto.CouponVerifyVO;
import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.entity.CouponTemplate;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {
    CouponTemplate createTemplate(CouponTemplate template);

    CouponTemplate createTemplate(CouponTemplate template, Long merchantId);

    CouponTemplate updateTemplate(CouponTemplate template);

    CouponTemplate updateTemplate(CouponTemplate template, Long merchantId);

    Page<CouponTemplate> listTemplates(Integer status, int page, int size);

    Page<CouponTemplate> listTemplates(Integer status, int page, int size, Long merchantId);

    void claim(Long userId, Long templateId);

    List<CouponVO> listAvailableCoupons();

    Page<CouponVO> listAvailableCoupons(int page, int size);

    List<CouponVO> listUserCoupons(Long userId, Integer status);

    Page<CouponVO> listUserCoupons(Long userId, Integer status, int page, int size);

    CouponVerifyVO verify(Long userCouponId, Long userId, BigDecimal orderAmount);

    void use(Long userCouponId, String orderNo);
}
