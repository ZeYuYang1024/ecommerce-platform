package com.ecommerce.logistics.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.logistics.common.LogisticsErrorCode;
import com.ecommerce.logistics.dto.request.BatchShipRequest;
import com.ecommerce.logistics.dto.request.CreateShippingTemplateRequest;
import com.ecommerce.logistics.dto.request.UpdateShippingTemplateRequest;
import com.ecommerce.logistics.dto.response.LogisticsProviderVO;
import com.ecommerce.logistics.dto.response.ShippingOrderVO;
import com.ecommerce.logistics.dto.response.ShippingTemplateVO;
import com.ecommerce.logistics.service.LogisticsProviderService;
import com.ecommerce.logistics.service.ShippingService;
import com.ecommerce.logistics.service.ShippingTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminLogisticsControllerTest {

    @Mock
    private LogisticsProviderService providerService;

    @Mock
    private ShippingService shippingService;

    @Mock
    private ShippingTemplateService templateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminLogisticsController(providerService, shippingService, templateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUseMerchantHeaderForShippingList() throws Exception {
        IPage<ShippingOrderVO> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(shippingService.listShipping(1, 20, "ORD-1", null, 88L)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/logistics/shipping")
                        .param("page", "1")
                        .param("size", "20")
                        .param("orderNo", "ORD-1")
                        .param("merchantId", "999")
                        .header("X-Merchant-Id", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(shippingService).listShipping(1, 20, "ORD-1", null, 88L);
    }

    @Test
    void shouldUseMerchantHeaderForTemplateList() throws Exception {
        IPage<ShippingTemplateVO> page = new Page<>(1, 20, 0);
        page.setRecords(Collections.emptyList());
        when(templateService.listTemplates(1, 20, 66L)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/logistics/templates")
                        .param("page", "1")
                        .param("size", "20")
                        .param("merchantId", "999")
                        .header("X-Merchant-Id", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(templateService).listTemplates(1, 20, 66L);
    }

    @Test
    void shouldRejectMerchantShippingListWithoutMerchantHeader() throws Exception {
        mockMvc.perform(get("/api/v1/admin/logistics/shipping")
                        .param("merchantId", "999")
                        .header("X-User-Type", "merchant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(LogisticsErrorCode.SHIPPING_FORBIDDEN.getCode()));
    }

    @Test
    void shouldRejectMerchantTemplateCreateWithoutMerchantHeader() throws Exception {
        mockMvc.perform(post("/api/v1/admin/logistics/templates")
                        .header("X-User-Type", "merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "merchant-template",
                                  "merchantId": 999,
                                  "calcType": 0,
                                  "firstUnit": 1,
                                  "firstFee": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(LogisticsErrorCode.TEMPLATE_FORBIDDEN.getCode()));
    }

    @Test
    void shouldUseMerchantHeaderForTemplateDetail() throws Exception {
        ShippingTemplateVO vo = new ShippingTemplateVO();
        vo.setId(5L);
        vo.setMerchantId(66L);
        when(templateService.getTemplate(5L, 66L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/admin/logistics/templates/5")
                        .header("X-Merchant-Id", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.merchantId").value(66));

        verify(templateService).getTemplate(5L, 66L);
    }

    @Test
    void shouldOverrideTemplateMerchantIdFromHeader() throws Exception {
        ShippingTemplateVO vo = new ShippingTemplateVO();
        vo.setId(1L);
        vo.setMerchantId(88L);
        when(templateService.createTemplate(any(CreateShippingTemplateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/v1/admin/logistics/templates")
                        .header("X-Merchant-Id", "88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "merchant-template",
                                  "merchantId": 999,
                                  "calcType": 0,
                                  "firstUnit": 1,
                                  "firstFee": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value(88));

        ArgumentCaptor<CreateShippingTemplateRequest> captor = ArgumentCaptor.forClass(CreateShippingTemplateRequest.class);
        verify(templateService).createTemplate(captor.capture());
        assertThat(captor.getValue().getMerchantId()).isEqualTo(88L);
    }

    @Test
    void shouldOverrideTemplateMerchantIdFromHeaderOnUpdate() throws Exception {
        ShippingTemplateVO vo = new ShippingTemplateVO();
        vo.setId(1L);
        vo.setMerchantId(88L);
        when(templateService.updateTemplate(eq(1L), any(UpdateShippingTemplateRequest.class), eq(88L))).thenReturn(vo);

        mockMvc.perform(put("/api/v1/admin/logistics/templates/1")
                        .header("X-Merchant-Id", "88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "updated-template",
                                  "merchantId": 999,
                                  "calcType": 0,
                                  "firstUnit": 1,
                                  "firstFee": 8.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantId").value(88));

        ArgumentCaptor<UpdateShippingTemplateRequest> captor = ArgumentCaptor.forClass(UpdateShippingTemplateRequest.class);
        verify(templateService).updateTemplate(eq(1L), captor.capture(), eq(88L));
        assertThat(captor.getValue().getMerchantId()).isEqualTo(88L);
    }

    @Test
    void shouldAllowPartialTemplateUpdate() throws Exception {
        ShippingTemplateVO vo = new ShippingTemplateVO();
        vo.setId(1L);
        vo.setMerchantId(88L);
        when(templateService.updateTemplate(eq(1L), any(UpdateShippingTemplateRequest.class), eq(88L))).thenReturn(vo);

        mockMvc.perform(put("/api/v1/admin/logistics/templates/1")
                        .header("X-Merchant-Id", "88")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstFee": 12.50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(templateService).updateTemplate(eq(1L), any(UpdateShippingTemplateRequest.class), eq(88L));
    }

    @Test
    void shouldUseMerchantHeaderForTemplateDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/logistics/templates/9")
                        .header("X-Merchant-Id", "66"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(templateService).deleteTemplate(9L, 66L);
    }

    @Test
    void shouldPassMerchantHeaderIntoBatchShip() throws Exception {
        ShippingOrderVO vo = new ShippingOrderVO();
        vo.setId(1L);
        when(shippingService.batchShip(any(BatchShipRequest.class), eq("super_admin"), eq(77L)))
                .thenReturn(List.of(vo));

        mockMvc.perform(post("/api/v1/admin/logistics/shipping/batch")
                        .header("X-Merchant-Id", "77")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "orderId": 1001,
                                      "providerId": 1,
                                      "trackingNo": "SF1234567890",
                                      "packageWeight": 500
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(shippingService).batchShip(any(BatchShipRequest.class), eq("super_admin"), eq(77L));
    }

    @Test
    void shouldPassMerchantHeaderIntoWaybillGeneration() throws Exception {
        when(shippingService.generateWaybill(3L, 77L)).thenReturn("https://waybill.example.com/SF123.pdf");

        mockMvc.perform(post("/api/v1/admin/logistics/shipping/3/waybill")
                        .header("X-Merchant-Id", "77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("https://waybill.example.com/SF123.pdf"));

        verify(shippingService).generateWaybill(3L, 77L);
    }
}
