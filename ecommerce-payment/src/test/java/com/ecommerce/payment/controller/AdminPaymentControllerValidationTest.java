package com.ecommerce.payment.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.payment.service.PaymentService;
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
class AdminPaymentControllerValidationTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void retryOutboxShouldRejectMissingMessageId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payment/outbox/retry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("messageId is required"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void retryOutboxBatchShouldRejectMissingLimit() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payment/outbox/retry-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic": "order-paid"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("limit is required"));

        verifyNoInteractions(paymentService);
    }

    @Test
    void retryOutboxBatchShouldRejectMissingFilter() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payment/outbox/retry-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "limit": 20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("retry batch requires a filter"));

        verifyNoInteractions(paymentService);
    }
}
