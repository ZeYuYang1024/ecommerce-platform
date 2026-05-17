package com.ecommerce.product.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.product.common.ProductErrorCode;
import com.ecommerce.product.entity.Brand;
import com.ecommerce.product.service.BrandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantBrandControllerTest {

    @Mock
    private BrandService brandService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantBrandController(brandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void detail_shouldRejectCrossTenantMerchantBrand() throws Exception {
        Brand foreignBrand = new Brand();
        foreignBrand.setId(3001L);
        foreignBrand.setName("foreign-brand");
        foreignBrand.setSourceType("merchant");
        foreignBrand.setMerchantId(3002L);
        when(brandService.getById(3001L)).thenReturn(foreignBrand);

        mockMvc.perform(get("/api/v1/admin/merchant/brands/3001")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ProductErrorCode.BRAND_FORBIDDEN.getCode()));

        verify(brandService).getById(3001L);
    }
}
