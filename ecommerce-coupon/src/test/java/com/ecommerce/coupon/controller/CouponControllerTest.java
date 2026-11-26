package com.ecommerce.coupon.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.coupon.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    @Mock
    private CouponService couponService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CouponController(couponService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mineSummariesShouldRequireCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/mine/summaries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(couponService);
    }

    @Test
    void mineSummariesShouldDelegateToSummaryReadPath() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/mine/summaries")
                        .header("X-User-Id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        assertThat(mockingDetails(couponService).getInvocations())
                .singleElement()
                .satisfies(invocation ->
                        assertThat(invocation.getMethod().getName()).isEqualTo("listCurrentUserCouponSummaries"));
    }

    @Test
    void availableCouponsUseThePublicListEvenWithAUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/available")
                        .param("page", "2")
                        .param("size", "5")
                        .header("X-User-Id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(couponService).listAvailableCoupons(2, 5);
    }

    @Test
    void mineRequiresTheCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        verifyNoInteractions(couponService);
    }

    @Test
    void mineUsesTheCurrentUserAndPageFilters() throws Exception {
        mockMvc.perform(get("/api/v1/coupons/mine")
                        .param("page", "3")
                        .param("size", "5")
                        .param("status", "0")
                        .header("X-User-Id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(couponService).listUserCoupons(2001L, 0, 3, 5);
    }
}
