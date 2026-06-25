package com.ecommerce.order.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerValidationTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(orderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateStatusShouldRejectMissingStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatusShouldRejectOutOfRangeStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 99
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatusShouldRejectLegacyRefundedStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatusShouldRejectManualShippedStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatusShouldRejectManualCompletedStatus() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));

        verifyNoInteractions(orderService);
    }

    @Test
    void updateStatusShouldRejectMerchantWithoutMerchantHeader() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/status")
                        .header("X-User-Type", "merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40010007));

        verifyNoInteractions(orderService);
    }

    @Test
    void orderShipEndpointShouldNotExist() throws Exception {
        mockMvc.perform(put("/api/v1/admin/orders/1/ship"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(orderService);
    }

    @Test
    void retryOutboxShouldRejectMissingMessageId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/outbox/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("messageId is required"));

        verifyNoInteractions(orderService);
    }

    @Test
    void retryOutboxBatchShouldRejectMissingLimit() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/outbox/retry-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "order-created"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("limit is required"));

        verifyNoInteractions(orderService);
    }

    @Test
    void retryOutboxBatchShouldRejectMissingFilter() throws Exception {
        mockMvc.perform(post("/api/v1/admin/orders/outbox/retry-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "limit": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("retry batch requires a filter"));

        verifyNoInteractions(orderService);
    }
}
