package com.ecommerce.coupon.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.coupon.entity.CouponTemplate;
import com.ecommerce.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCouponControllerTest {

    @Mock
    private CouponService couponService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminCouponController(couponService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantListShouldUseMerchantScopedService() throws Exception {
        CouponTemplate template = new CouponTemplate();
        template.setId(1001L);
        template.setName("merchant-coupon");
        Page<CouponTemplate> page = new Page<>(1, 10);
        page.setRecords(List.of(template));
        page.setTotal(1);

        when(couponService.listTemplates(1, 1, 10, 2001L)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/coupons")
                        .header("X-Merchant-Id", "2001")
                        .param("status", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].name").value("merchant-coupon"));

        verify(couponService).listTemplates(1, 1, 10, 2001L);
    }

    @Test
    void merchantCreateShouldBindMerchantId() throws Exception {
        CouponTemplate created = new CouponTemplate();
        created.setId(1002L);
        created.setName("merchant-coupon");
        created.setMerchantId(2001L);
        created.setDiscountAmount(new BigDecimal("10.00"));

        when(couponService.createTemplate(any(CouponTemplate.class), eq(2001L))).thenReturn(created);

        mockMvc.perform(post("/api/v1/admin/merchant/coupons")
                        .header("X-Merchant-Id", "2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "merchant-coupon",
                                  "type": "FLAT",
                                  "minAmount": 100,
                                  "discountAmount": 10,
                                  "totalCount": 50,
                                  "remainingCount": 50,
                                  "perUserLimit": 1,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value("2001"));

        verify(couponService).createTemplate(any(CouponTemplate.class), eq(2001L));
    }
}
