package com.ecommerce.merchant.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.merchant.dto.request.MerchantUpdateRequest;
import com.ecommerce.merchant.dto.response.MerchantVO;
import com.ecommerce.merchant.service.MerchantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminMerchantControllerTest {

    private static final int MERCHANT_PERMISSION_DENIED = 60010006;

    @Mock
    private MerchantService merchantService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminMerchantController(merchantService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantAdminShouldReachOwnMerchantDetail() throws Exception {
        MerchantVO merchant = new MerchantVO();
        merchant.setId(2001L);
        merchant.setName("Own Shop");
        when(merchantService.getById(2001L)).thenReturn(merchant);

        mockMvc.perform(get("/api/v1/admin/merchants/2001")
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("2001"))
                .andExpect(jsonPath("$.data.name").value("Own Shop"));

        verify(merchantService).getById(2001L);
    }

    @Test
    void merchantAdminShouldBeForbiddenFromOtherMerchantDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/merchants/3001")
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MERCHANT_PERMISSION_DENIED));

        verifyNoInteractions(merchantService);
    }

    @Test
    void merchantAdminShouldBeForbiddenFromOtherMerchantUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/admin/merchants/3001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "2001")
                        .content("""
                                {
                                  "name":"Hack Shop",
                                  "logo":"",
                                  "contactName":"Hacker",
                                  "contactPhone":"13800000000",
                                  "businessLicense":"test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(MERCHANT_PERMISSION_DENIED));

        verify(merchantService, never()).update(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void platformAdminShouldStillReachMerchantDetail() throws Exception {
        MerchantVO merchant = new MerchantVO();
        merchant.setId(3001L);
        merchant.setName("Platform View");
        when(merchantService.getById(3001L)).thenReturn(merchant);

        mockMvc.perform(get("/api/v1/admin/merchants/3001")
                        .header("X-User-Type", "super_admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Platform View"));

        verify(merchantService).getById(3001L);
    }
}
