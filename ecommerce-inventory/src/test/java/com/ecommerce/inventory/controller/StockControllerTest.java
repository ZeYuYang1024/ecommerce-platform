package com.ecommerce.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.inventory.dto.response.InventoryEventLogVO;
import com.ecommerce.inventory.dto.response.InventoryEventSummaryVO;
import com.ecommerce.inventory.dto.request.StockSetRequest;
import com.ecommerce.inventory.dto.response.StockVO;
import com.ecommerce.inventory.service.InventoryEventAdminService;
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

    @Mock
    private InventoryEventAdminService inventoryEventAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StockController(stockService, inventoryEventAdminService)).build();
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

    @Test
    void legacyAdminInventoryAliasShouldNotBeMapped() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/admin/list"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicDeductRouteShouldNotBeMapped() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skuId": 100,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void internalDeductRouteShouldBeMapped() throws Exception {
        mockMvc.perform(post("/api/v1/internal/inventory/deduct")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skuId": 100,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(stockService).deduct(100L, 2);
    }

    @Test
    void inventoryEventsShouldUseCanonicalAdminRoute() throws Exception {
        Page<InventoryEventLogVO> page = new Page<>(1, 10, 1);
        InventoryEventLogVO vo = new InventoryEventLogVO();
        vo.setId(3001L);
        vo.setTopic("order-created");
        vo.setOrderNo("ORD-1");
        vo.setStatus(1);
        page.setRecords(java.util.List.of(vo));
        when(inventoryEventAdminService.listEvents("order-created", "ORD-1", 1, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/inventory/events")
                        .param("topic", "order-created")
                        .param("orderNo", "ORD-1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD-1"));

        verify(inventoryEventAdminService).listEvents("order-created", "ORD-1", 1, 1, 10);
    }

    @Test
    void inventoryEventSummaryShouldUseCanonicalAdminRoute() throws Exception {
        when(inventoryEventAdminService.summarize("order-created", "ORD-1", 1))
                .thenReturn(new InventoryEventSummaryVO(1, 2));

        mockMvc.perform(get("/api/v1/admin/inventory/events/summary")
                        .param("topic", "order-created")
                        .param("orderNo", "ORD-1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.processingCount").value(1))
                .andExpect(jsonPath("$.data.processedCount").value(2));

        verify(inventoryEventAdminService).summarize("order-created", "ORD-1", 1);
    }
}
