package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.PaymentClient;
import com.ecommerce.knowledge.client.dto.PaymentVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryToolTest {

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private PaymentQueryTool paymentQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserPaymentByOrderNoReturnsPaymentForCurrentUser() {
        AgentUserContextHolder.set(new AgentUserContext(1004L, "USER"));
        PaymentVO payment = new PaymentVO();
        payment.setOrderNo("ORD-1");
        payment.setAmount(new BigDecimal("88.00"));
        when(paymentClient.getPaymentByOrderNo(1004L, "ORD-1")).thenReturn(Result.ok(payment));

        PaymentVO result = paymentQueryTool.queryCurrentUserPaymentByOrderNo("ORD-1");

        assertEquals("ORD-1", result.getOrderNo());
        verify(paymentClient).getPaymentByOrderNo(1004L, "ORD-1");
    }

    @Test
    void queryCurrentUserPaymentByOrderNoReturnsNullWhenCurrentUserMissing() {
        PaymentVO result = paymentQueryTool.queryCurrentUserPaymentByOrderNo("ORD-1");

        assertNull(result);
        verify(paymentClient, never()).getPaymentByOrderNo(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void queryCurrentUserPaymentByOrderNoReturnsNullWhenClientThrows() {
        AgentUserContextHolder.set(new AgentUserContext(1004L, "USER"));
        when(paymentClient.getPaymentByOrderNo(1004L, "ORD-1")).thenThrow(new RuntimeException("boom"));

        PaymentVO result = paymentQueryTool.queryCurrentUserPaymentByOrderNo("ORD-1");

        assertNull(result);
    }
}
