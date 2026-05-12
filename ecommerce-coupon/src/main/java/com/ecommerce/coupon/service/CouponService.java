package com.ecommerce.coupon.service;

import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.coupon.entity.CouponTemplate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CouponService {
    // 模板管理（admin）
    CouponTemplate createTemplate(CouponTemplate template);
    CouponTemplate updateTemplate(CouponTemplate template);
    List<CouponTemplate> listTemplates(Integer status);

    // 用户领券
    void claim(Long userId, Long templateId);

    // 用户券列表
    List<CouponVO> listUserCoupons(Long userId, Integer status);

    // 下单校验（内部调用）
    Map<String, Object> verify(Long userCouponId, Long userId, BigDecimal orderAmount);

    // 核销（下单后调用）
    void use(Long userCouponId, String orderNo);
}
