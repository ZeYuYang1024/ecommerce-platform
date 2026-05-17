package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.payment.dto.response.SettlementVO;
import com.ecommerce.payment.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantSettlementControllerTest {

    @Mock
    private SettlementService settlementService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantSettlementController(settlementService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListMerchantSettlements() throws Exception {
        SettlementVO settlement = new SettlementVO();
        settlement.setSettlementDate(LocalDate.of(2026, 5, 15));
        Page<SettlementVO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(settlement));
        when(settlementService.listByMerchant(2001L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/settlement")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].settlementDate").value("2026-05-15"));

        verify(settlementService).listByMerchant(2001L, 1, 10);
    }
}
