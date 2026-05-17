package com.ecommerce.seckill.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.seckill.entity.SeckillItem;
import com.ecommerce.seckill.entity.SeckillSession;
import com.ecommerce.seckill.service.SeckillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
class AdminSeckillControllerTest {

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
    void merchantSessionListShouldUseMerchantScopedService() throws Exception {
        SeckillSession session = new SeckillSession();
        session.setId(7001L);
        session.setName("merchant-session");
        session.setMerchantId(2001L);
        Page<SeckillSession> page = new Page<>(1, 10);
        page.setRecords(List.of(session));
        page.setTotal(1);

        when(seckillService.listSessionsByMerchant(2001L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/seckill/sessions")
                        .header("X-Merchant-Id", "2001")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].merchantId").value("2001"));

        verify(seckillService).listSessionsByMerchant(2001L, 1, 10);
    }

    @Test
    void merchantItemCreateShouldBindMerchantId() throws Exception {
        SeckillItem item = new SeckillItem();
        item.setId(8001L);
        item.setMerchantId(2001L);
        item.setName("merchant-item");

        when(seckillService.createItem(any(SeckillItem.class), eq(2001L))).thenReturn(item);

        mockMvc.perform(post("/api/v1/admin/merchant/seckill/items")
                        .header("X-Merchant-Id", "2001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": 7001,
                                  "spuId": 11,
                                  "skuId": 12,
                                  "name": "merchant-item",
                                  "originalPrice": 99,
                                  "seckillPrice": 59,
                                  "stockCount": 10,
                                  "remainingCount": 10,
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value("2001"));

        verify(seckillService).createItem(any(SeckillItem.class), eq(2001L));
    }
}
