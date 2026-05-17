package com.ecommerce.coupon.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.coupon.entity.CouponTemplate;
import com.ecommerce.coupon.mapper.CouponTemplateMapper;
import com.ecommerce.coupon.mapper.UserCouponMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
