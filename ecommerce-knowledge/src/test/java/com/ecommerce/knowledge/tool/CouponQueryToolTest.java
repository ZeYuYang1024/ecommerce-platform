package com.ecommerce.knowledge.tool;

import com.ecommerce.knowledge.agent.AgentUserContext;
import com.ecommerce.knowledge.agent.AgentUserContextHolder;
import com.ecommerce.knowledge.client.CouponClient;
import com.ecommerce.knowledge.client.dto.CouponVO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponQueryToolTest {

    @Mock
    private CouponClient couponClient;

    @InjectMocks
    private CouponQueryTool couponQueryTool;

    @AfterEach
    void tearDown() {
        AgentUserContextHolder.clear();
    }

    @Test
    void queryCurrentUserCouponsUsesSummariesEndpoint() {
        AgentUserContextHolder.set(new AgentUserContext(1002L, "USER"));
        CouponVO coupon = new CouponVO();
        coupon.setName("满100减10");
        when(couponClient.listMySummaries(1002L)).thenReturn(Result.ok(List.of(coupon)));

        List<CouponVO> result = couponQueryTool.queryCurrentUserCoupons();

        assertEquals(1, result.size());
        assertEquals("满100减10", result.getFirst().getName());
        verify(couponClient).listMySummaries(1002L);
        verify(couponClient, never()).listMine(anyLong(), any(), anyInt(), anyInt());
    }

    @Test
    void queryCurrentUserCouponsReturnsEmptyListWhenCurrentUserMissing() {
        List<CouponVO> result = couponQueryTool.queryCurrentUserCoupons();

        assertTrue(result.isEmpty());
        verify(couponClient, never()).listMySummaries(anyLong());
    }
}
