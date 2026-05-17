package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.payment.dto.response.ReconciliationVO;
import com.ecommerce.payment.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantReconciliationControllerTest {

    @Mock
    private ReconciliationService reconciliationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantReconciliationController(reconciliationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListMerchantReconciliationBatches() throws Exception {
        ReconciliationVO row = new ReconciliationVO();
        row.setId(1L);
        row.setBatchNo("REC001");
        Page<ReconciliationVO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(row));
        when(reconciliationService.listByMerchant(2001L, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/reconciliation")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].batchNo").value("REC001"));

        verify(reconciliationService).listByMerchant(2001L, 1, 10);
    }

    @Test
    void shouldGetMerchantReconciliationDetail() throws Exception {
        ReconciliationVO detail = new ReconciliationVO();
        detail.setId(1L);
        detail.setBatchNo("REC001");
        when(reconciliationService.getReconciliationDetailByMerchant(2001L, 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/merchant/reconciliation/1")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.batchNo").value("REC001"));

        verify(reconciliationService).getReconciliationDetailByMerchant(2001L, 1L);
    }

    @Test
    void shouldReturnExplicitFailureWhenMerchantReconciliationDetailIsNotVisible() throws Exception {
        when(reconciliationService.getReconciliationDetailByMerchant(2001L, 99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/admin/merchant/reconciliation/99")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50010006));

        verify(reconciliationService).getReconciliationDetailByMerchant(2001L, 99L);
    }
}
