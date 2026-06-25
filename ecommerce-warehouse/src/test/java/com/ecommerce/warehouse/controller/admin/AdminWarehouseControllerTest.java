package com.ecommerce.warehouse.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.warehouse.common.WarehouseErrorCode;
import com.ecommerce.warehouse.dto.request.CreateWarehouseRequest;
import com.ecommerce.warehouse.dto.request.UpdateWarehouseRequest;
import com.ecommerce.warehouse.dto.response.InboundOrderVO;
import com.ecommerce.warehouse.dto.response.WarehouseVO;
import com.ecommerce.warehouse.service.CheckService;
import com.ecommerce.warehouse.service.InboundService;
import com.ecommerce.warehouse.service.OutboundService;
import com.ecommerce.warehouse.service.StockService;
import com.ecommerce.warehouse.service.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminWarehouseControllerTest {

    @Mock
    private WarehouseService warehouseService;
    @Mock
    private InboundService inboundService;
    @Mock
    private OutboundService outboundService;
    @Mock
    private StockService stockService;
    @Mock
    private CheckService checkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminWarehouseController(
                        warehouseService, inboundService, outboundService, stockService, checkService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUseMerchantHeaderForWarehouseList() throws Exception {
        IPage<WarehouseVO> page = new Page<>(1, 20, 0);
        when(warehouseService.listWarehouses(1, 20, 88L)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/warehouses")
                        .param("page", "1")
                        .param("size", "20")
                        .param("merchantId", "12")
                        .header("X-Merchant-Id", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(warehouseService).listWarehouses(1, 20, 88L);
    }

    @Test
    void shouldRejectMerchantWarehouseListWithoutMerchantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/admin/warehouses")
                        .param("merchantId", "12")
                        .header("X-User-Type", "merchant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));
    }

    @Test
    void shouldOverrideWarehouseMerchantIdFromHeader() throws Exception {
        WarehouseVO warehouse = new WarehouseVO();
        warehouse.setId(1L);
        warehouse.setMerchantId(66L);
        when(warehouseService.createWarehouse(any(CreateWarehouseRequest.class))).thenReturn(warehouse);

        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", "66")
                        .content("""
                                {
                                  "warehouseName": "Main",
                                  "warehouseCode": "WH001",
                                  "warehouseType": 1,
                                  "stockMode": 0,
                                  "merchantId": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.merchantId").value(66));

        verify(warehouseService).createWarehouse(any(CreateWarehouseRequest.class));
    }

    @Test
    void shouldRejectMerchantWarehouseCreateWithoutMerchantHeader() throws Exception {
        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Type", "merchant")
                        .content("""
                                {
                                  "warehouseName": "Main",
                                  "warehouseCode": "WH001",
                                  "warehouseType": 1,
                                  "stockMode": 0,
                                  "merchantId": 12
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));
    }

    @Test
    void shouldRejectManagedWarehouseCreateForMerchant() throws Exception {
        mockMvc.perform(post("/api/v1/admin/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "66")
                        .content("""
                                {
                                  "warehouseName": "Managed",
                                  "warehouseCode": "WH002",
                                  "warehouseType": 1,
                                  "stockMode": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));
    }

    @Test
    void shouldRejectWarehouseAccessForDifferentMerchant() throws Exception {
        WarehouseVO warehouse = new WarehouseVO();
        warehouse.setId(1L);
        warehouse.setMerchantId(66L);
        when(warehouseService.getWarehouse(1L)).thenReturn(warehouse);

        mockMvc.perform(get("/api/v1/admin/warehouses/1")
                        .header("X-Merchant-Id", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));
    }

    @Test
    void shouldAllowPartialWarehouseUpdate() throws Exception {
        WarehouseVO warehouse = new WarehouseVO();
        warehouse.setId(1L);
        warehouse.setMerchantId(66L);
        when(warehouseService.getWarehouse(1L)).thenReturn(warehouse);
        when(warehouseService.updateWarehouse(eq(1L), any(UpdateWarehouseRequest.class))).thenReturn(warehouse);

        mockMvc.perform(put("/api/v1/admin/warehouses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Merchant-Id", "66")
                        .content("""
                                {
                                  "contactPhone": "13900139000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(warehouseService).updateWarehouse(eq(1L), any(UpdateWarehouseRequest.class));
    }

    @Test
    void shouldRejectMerchantWarehouseDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/warehouses/1")
                        .header("X-User-Type", "merchant")
                        .header("X-Merchant-Id", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));

        verify(warehouseService, never()).deleteWarehouse(1L);
    }

    @Test
    void shouldRejectInboundAccessForDifferentMerchant() throws Exception {
        InboundOrderVO inbound = new InboundOrderVO();
        inbound.setId(10L);
        inbound.setMerchantId(66L);
        when(inboundService.getInbound(10L)).thenReturn(inbound);

        mockMvc.perform(get("/api/v1/admin/warehouse/inbounds/10")
                        .header("X-Merchant-Id", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(WarehouseErrorCode.WAREHOUSE_FORBIDDEN.getCode()));
    }
}
