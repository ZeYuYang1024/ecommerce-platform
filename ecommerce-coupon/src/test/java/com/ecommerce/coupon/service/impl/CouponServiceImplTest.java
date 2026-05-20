package com.ecommerce.coupon.service.impl;

import com.ecommerce.coupon.dto.response.CouponVO;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.coupon.entity.CouponTemplate;
import com.ecommerce.coupon.entity.UserCoupon;
import com.ecommerce.coupon.mapper.CouponTemplateMapper;
import com.ecommerce.coupon.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock
    private CouponTemplateMapper templateMapper;

    @Mock
    private UserCouponMapper userCouponMapper;

    @InjectMocks
    private CouponServiceImpl couponService;

    @Test
    void updateTemplate_shouldRejectCrossTenantMerchantUpdate() {
        CouponTemplate existing = new CouponTemplate();
        existing.setId(1001L);
        existing.setMerchantId(3002L);
        existing.setName("foreign-template");
        when(templateMapper.selectById(1001L)).thenReturn(existing);

        CouponTemplate incoming = new CouponTemplate();
        incoming.setId(1001L);
        incoming.setName("hack-template");

        assertThatThrownBy(() -> couponService.updateTemplate(incoming, 2001L))
                .isInstanceOf(BusinessException.class);

        verify(templateMapper, never()).updateById(any(CouponTemplate.class));
    }

    @Test
    void listCurrentUserCouponSummaries_shouldReturnCompactCouponsForCurrentUser() {
        UserCoupon first = new UserCoupon();
        first.setId(7001L);
        first.setUserId(2001L);
        first.setTemplateId(3001L);
        first.setStatus(0);

        UserCoupon second = new UserCoupon();
        second.setId(7002L);
        second.setUserId(2001L);
        second.setTemplateId(3001L);
        second.setStatus(1);

        CouponTemplate template = new CouponTemplate();
        template.setId(3001L);
        template.setName("618 coupon");
        template.setType("FLAT");
        template.setMinAmount(new BigDecimal("100.00"));
        template.setDiscountAmount(new BigDecimal("10.00"));
        template.setStartTime(LocalDateTime.of(2026, 5, 1, 0, 0));
        template.setEndTime(LocalDateTime.of(2026, 6, 1, 0, 0));

        when(userCouponMapper.selectList(any())).thenReturn(List.of(first, second));
        when(templateMapper.selectById(3001L)).thenReturn(template);

        List<CouponVO> result = invokeCurrentUserCouponSummaries(2001L);

        assertThat(result)
                .hasSize(2)
                .extracting(CouponVO::getUserCouponId, CouponVO::getId, CouponVO::getName, CouponVO::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(7001L, 3001L, "618 coupon", 0),
                        org.assertj.core.groups.Tuple.tuple(7002L, 3001L, "618 coupon", 1)
                );
        verify(templateMapper).selectById(3001L);
    }

    @SuppressWarnings("unchecked")
    private List<CouponVO> invokeCurrentUserCouponSummaries(Long userId) {
        try {
            Method method = CouponServiceImpl.class.getMethod("listCurrentUserCouponSummaries", Long.class);
            return (List<CouponVO>) method.invoke(couponService, userId);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Expected summary-oriented current-user coupon read path", e);
        }
    }
}
