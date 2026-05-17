package com.ecommerce.seckill.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.seckill.service.SeckillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSeckillControllerValidationTest {

    @Mock
    private SeckillService seckillService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminSeckillController(seckillService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void merchantCreateSessionShouldRejectMissingName() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/seckill/sessions")
                        .header("X-Merchant-Id", "2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verifyNoInteractions(seckillService);
    }

    @Test
    void merchantCreateItemShouldRejectMissingName() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/seckill/items")
                        .header("X-Merchant-Id", "2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": 11,
                                  "spuId": 21,
                                  "skuId": 31,
                                  "originalPrice": 199.00,
                                  "seckillPrice": 99.00,
                                  "stockCount": 50,
                                  "remainingCount": 50,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name")));

        verifyNoInteractions(seckillService);
    }
}
