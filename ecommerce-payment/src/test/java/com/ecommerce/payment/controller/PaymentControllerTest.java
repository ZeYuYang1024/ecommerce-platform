package com.ecommerce.payment.controller;

import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.payment.common.PaymentErrorCode;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldQueryCurrentUserPaymentByOrderNo() throws Exception {
        PaymentVO payment = new PaymentVO();
        payment.setId(1L);
        payment.setPaymentNo("PAY202605091200000001");
        payment.setOrderNo("202605091200000001");
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("6999.00"));
        payment.setStatus(1);

        when(paymentService.queryByOrderNoForUser(1L, "202605091200000001")).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payment/orders/202605091200000001")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.paymentNo").value("PAY202605091200000001"))
                .andExpect(jsonPath("$.data.orderNo").value("202605091200000001"))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(paymentService).queryByOrderNoForUser(1L, "202605091200000001");
    }

    @Test
    void shouldRequireUserIdHeaderForCurrentUserPaymentQuery() throws Exception {
        mockMvc.perform(get("/api/v1/payment/orders/202605091200000001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldRequireUserIdHeaderForLegacyQueryEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/payment/202605091200000001"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldScopeLegacyQueryEndpointWhenUserIdHeaderPresent() throws Exception {
        PaymentVO payment = new PaymentVO();
        payment.setPaymentNo("PAY202605091200000001");
        payment.setOrderNo("202605091200000001");
        payment.setUserId(1L);

        when(paymentService.queryByOrderNoForUser(1L, "202605091200000001")).thenReturn(payment);

        mockMvc.perform(get("/api/v1/payment/202605091200000001")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1));

        verify(paymentService).queryByOrderNoForUser(1L, "202605091200000001");
        verify(paymentService, never()).queryByOrderNo("202605091200000001");
    }

    @Test
    void shouldReturnNotFoundForScopedQueryWhenOrderBelongsToDifferentUser() throws Exception {
        when(paymentService.queryByOrderNoForUser(2L, "202605091200000001"))
                .thenThrow(new BusinessException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/payment/orders/202605091200000001")
                        .header("X-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(PaymentErrorCode.PAYMENT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(PaymentErrorCode.PAYMENT_NOT_FOUND.getMessage()));

        verify(paymentService).queryByOrderNoForUser(2L, "202605091200000001");
    }
}
