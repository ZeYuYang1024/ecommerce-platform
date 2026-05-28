package com.ecommerce.order.controller;

import com.ecommerce.common.dto.OrderInternalVO;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.dto.response.OrderSummaryVO;
import com.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, orderMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldListMerchantOrderNosForInternalUse() throws Exception {
        when(orderService.listOrderNosByMerchant(100L)).thenReturn(List.of("ORD001", "ORD002"));

        mockMvc.perform(get("/api/v1/internal/orders/merchant/order-nos")
                        .param("merchantId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("ORD001"))
                .andExpect(jsonPath("$.data[1]").value("ORD002"));

        verify(orderService).listOrderNosByMerchant(100L);
    }

    @Test
    void shouldListCurrentUserOrderSummaries() throws Exception {
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setOrderNo("ORD-SUM-001");
        summary.setStatus(1);
        summary.setStatusText("PAID");
        summary.setTotalAmount(new BigDecimal("88.50"));
        summary.setCreatedAt(LocalDateTime.of(2026, 5, 20, 9, 30));
        summary.setFirstItemName("Phone");
        summary.setItemCount(2);
        when(orderService.listSummariesByUser(88L, 3)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/orders/summaries")
                        .header("X-User-Id", "88")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderNo").value("ORD-SUM-001"))
                .andExpect(jsonPath("$.data[0].status").value(1))
                .andExpect(jsonPath("$.data[0].statusText").value("PAID"))
                .andExpect(jsonPath("$.data[0].itemCount").value(2))
                .andExpect(jsonPath("$.data[0].firstItemName").value("Phone"));

        verify(orderService).listSummariesByUser(88L, 3);
    }

    @Test
    void shouldReturnFullInternalOrderPayloadByOrderNo() throws Exception {
        Order order = new Order();
        order.setId(101L);
        order.setOrderNo("ORD-INT-001");
        order.setUserId(88L);
        order.setTotalAmount(new BigDecimal("256.80"));
        order.setStatus(1);
        when(orderMapper.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(order);

        mockMvc.perform(get("/api/v1/internal/orders/no/ORD-INT-001")
                        .param("userId", "88"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.orderNo").value("ORD-INT-001"))
                .andExpect(jsonPath("$.data.totalAmount").value(256.80))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void shouldRequireUserIdHeaderForOrderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orders/123"))
                .andExpect(status().isBadRequest());
    }
}
