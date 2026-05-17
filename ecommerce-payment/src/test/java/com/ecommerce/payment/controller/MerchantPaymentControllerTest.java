package com.ecommerce.payment.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.payment.dto.request.RefundRequest;
import com.ecommerce.payment.dto.response.PaymentVO;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantPaymentController(paymentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListMerchantPayments() throws Exception {
        PaymentVO payment = new PaymentVO();
        payment.setPaymentNo("PAY001");
        payment.setOrderNo("ORD001");
        payment.setAmount(new BigDecimal("100.00"));

        Page<PaymentVO> page = new Page<>(1, 10, 1);
        page.setRecords(Collections.singletonList(payment));
        when(paymentService.listByMerchant(2001L, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/merchant/payment")
                        .header("X-Merchant-Id", 2001L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].paymentNo").value("PAY001"));

        verify(paymentService).listByMerchant(2001L, null, 1, 10);
    }

    @Test
    void shouldRefundMerchantPayment() throws Exception {
        PaymentVO payment = new PaymentVO();
        payment.setOrderNo("ORD001");
        payment.setStatus(3);

        RefundRequest request = new RefundRequest();
        request.setReason("merchant refund");

        when(paymentService.refundByMerchant(eq(2001L), eq("ORD001"), any(RefundRequest.class))).thenReturn(payment);

        mockMvc.perform(post("/api/v1/admin/merchant/payment/ORD001/refund")
                        .header("X-Merchant-Id", 2001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value(3));

        verify(paymentService).refundByMerchant(eq(2001L), eq("ORD001"), any(RefundRequest.class));
    }
}
