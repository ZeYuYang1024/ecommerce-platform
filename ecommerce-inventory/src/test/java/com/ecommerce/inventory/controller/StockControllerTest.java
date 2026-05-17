package com.ecommerce.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.request.StockSetRequest;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StockControllerTest {

    @Mock
    private StockService stockService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StockController(stockService)).build();
    }

    @Test
    void merchantInventoryListShouldUseCanonicalAdminRoute() throws Exception {
        Page<StockVO> page = new Page<>(1, 10);
        when(stockService.listForMerchant(2001L, null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/inventory")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(stockService).listForMerchant(2001L, null, null, 1, 10);
    }

    @Test
    void merchantInventoryUpdateShouldUseCanonicalAdminRoute() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchant/inventory/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", 2001L)
                        .content("""
                                {
                                  "totalStock": 88
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(stockService).setStockForMerchant(2001L, 100L, 88);
    }

    @Test
    void adminInventoryListShouldUseCanonicalAdminRoute() throws Exception {
        Page<StockVO> page = new Page<>(1, 10);
        when(stockService.list(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(stockService).list(null, null, 1, 10);
    }

    @Test
    void adminInventoryUpdateShouldUseCanonicalAdminRoute() throws Exception {
        mockMvc.perform(post("/api/v1/admin/inventory/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "totalStock": 66
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(stockService).setStock(100L, 66);
    }
}
