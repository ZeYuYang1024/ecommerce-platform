package com.ecommerce.monitor.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.monitor.dto.request.OutboxRetryRequest;
import com.ecommerce.monitor.dto.response.InventoryEventLogVO;
import com.ecommerce.monitor.dto.response.InventoryEventSummaryVO;
import com.ecommerce.monitor.dto.response.OutboxMessageVO;
import com.ecommerce.monitor.dto.response.ReliabilityOverviewVO;
import com.ecommerce.monitor.service.ReliabilityService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReliabilityControllerTest {

    @Mock
    private ReliabilityService reliabilityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ReliabilityController(reliabilityService),
                        new ReliabilityPageController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void overviewShouldReturnAggregatedPayload() throws Exception {
        ReliabilityOverviewVO overview = new ReliabilityOverviewVO();
        overview.setFailedOrderOutboxCount(4);
        overview.setFailedPaymentOutboxCount(2);
        overview.setExhaustedRetryCount(1);
        overview.setInventoryProcessedCount(18);
        overview.setInventoryProcessingCount(3);
        when(reliabilityService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/admin/api/reliability/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.failedOrderOutboxCount").value(4))
                .andExpect(jsonPath("$.data.failedPaymentOutboxCount").value(2));
    }

    @Test
    void listPaymentOutboxShouldDelegateToService() throws Exception {
        Page<OutboxMessageVO> page = new Page<>(1, 10, 1);
        OutboxMessageVO vo = new OutboxMessageVO();
        vo.setAggregateId("ORD-9");
        page.setRecords(java.util.List.of(vo));
        when(reliabilityService.listOutbox("payment", 3, "order-paid", "ORD-9", 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/api/reliability/outbox/payment/messages")
                        .param("status", "3")
                        .param("topic", "order-paid")
                        .param("aggregateId", "ORD-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].aggregateId").value("ORD-9"));

        verify(reliabilityService).listOutbox("payment", 3, "order-paid", "ORD-9", 1, 10);
    }

    @Test
    void retryOrderOutboxShouldDelegateToService() throws Exception {
        when(reliabilityService.retryOutboxMessage("order", 1001L)).thenReturn(1);

        mockMvc.perform(post("/admin/api/reliability/outbox/order/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messageId": 1001
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        verify(reliabilityService).retryOutboxMessage("order", 1001L);
    }

    @Test
    void retryOrderOutboxBatchShouldDelegateToService() throws Exception {
        when(reliabilityService.retryOutboxBatch("order", 3, "order-created", "ORD-1", 20)).thenReturn(2);

        mockMvc.perform(post("/admin/api/reliability/outbox/order/retry-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 3,
                                  "topic": "order-created",
                                  "aggregateId": "ORD-1",
                                  "limit": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        verify(reliabilityService).retryOutboxBatch("order", 3, "order-created", "ORD-1", 20);
    }

    @Test
    void listInventoryEventsShouldDelegateToService() throws Exception {
        Page<InventoryEventLogVO> page = new Page<>(1, 10, 1);
        InventoryEventLogVO vo = new InventoryEventLogVO();
        vo.setOrderNo("ORD-1");
        page.setRecords(java.util.List.of(vo));
        when(reliabilityService.listInventoryEvents("order-created", "ORD-1", 1, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/admin/api/reliability/inventory/events")
                        .param("topic", "order-created")
                        .param("orderNo", "ORD-1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].orderNo").value("ORD-1"));

        verify(reliabilityService).listInventoryEvents("order-created", "ORD-1", 1, 1, 10);
    }

    @Test
    void inventorySummaryShouldDelegateToService() throws Exception {
        when(reliabilityService.getInventorySummary("order-created", "ORD-1", 1))
                .thenReturn(new InventoryEventSummaryVO(1, 2));

        mockMvc.perform(get("/admin/api/reliability/inventory/events/summary")
                        .param("topic", "order-created")
                        .param("orderNo", "ORD-1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingCount").value(1))
                .andExpect(jsonPath("$.data.processedCount").value(2));

        verify(reliabilityService).getInventorySummary("order-created", "ORD-1", 1);
    }

    @Test
    void reliabilityPageShouldBeServed() throws Exception {
        mockMvc.perform(get("/admin/reliability"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/reliability/index.html"));
    }
}
