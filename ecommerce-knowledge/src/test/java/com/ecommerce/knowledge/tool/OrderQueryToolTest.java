package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.OrderClient;
import com.ecommerce.knowledge.client.dto.OrderSummaryVO;
import com.ecommerce.knowledge.client.dto.OrderVO;
import com.ecommerce.knowledge.common.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryToolTest {

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private OrderQueryTool orderQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserOrderSummariesReturnsSummaryList() {
        AgentUserContextHolder.set(new AgentUserContext(1002L, "USER"));
        OrderSummaryVO summary = new OrderSummaryVO();
        summary.setOrderNo("ORD-1");
        when(orderClient.listSummaries(1002L, 5)).thenReturn(Result.ok(List.of(summary)));

        List<OrderSummaryVO> result = orderQueryTool.queryCurrentUserOrderSummaries(5);

        assertEquals(1, result.size());
        assertEquals("ORD-1", result.getFirst().getOrderNo());
        verify(orderClient).listSummaries(1002L, 5);
        verify(orderClient, never()).listByUser(anyLong(), anyInt(), anyInt());
    }

    @Test
    void queryCurrentUserOrderSummariesReturnsEmptyListWhenCurrentUserMissing() {
        List<OrderSummaryVO> result = orderQueryTool.queryCurrentUserOrderSummaries(5);

        assertTrue(result.isEmpty());
        verify(orderClient, never()).listSummaries(anyLong(), anyInt());
    }

    @Test
    void queryOrderByNoStillUsesDetailEndpoint() {
        AgentUserContextHolder.set(new AgentUserContext(1002L, "USER"));
        OrderVO order = new OrderVO();
        order.setOrderNo("ORD-1");
        when(orderClient.getByOrderNo(1002L, "ORD-1")).thenReturn(Result.ok(order));

        OrderVO result = orderQueryTool.queryOrderByNo("ORD-1");

        assertEquals("ORD-1", result.getOrderNo());
        verify(orderClient).getByOrderNo(1002L, "ORD-1");
    }
}
