package com.ecommerce.member.service.impl;

import com.ecommerce.common.dto.OrderMemberVO;
import com.ecommerce.common.result.Result;
import com.ecommerce.member.client.OrderClient;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.mapper.GrowthTransactionMapper;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsConsumeDetailMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.GrowthService;
import com.ecommerce.member.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundCompensationServiceImplTest {

    @Mock
    private PointsReservationMapper pointsReservationMapper;

    @Mock
    private PointsConsumeDetailMapper pointsConsumeDetailMapper;

    @Mock
    private PointsTransactionMapper pointsTransactionMapper;

    @Mock
    private GrowthTransactionMapper growthTransactionMapper;

    @Mock
    private MemberProfileMapper memberProfileMapper;

    @Mock
    private PointsService pointsService;

    @Mock
    private GrowthService growthService;

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private RefundCompensationServiceImpl service;

    @Test
    void compensateShouldReverseFullRefundExactlyOnce() {
        PointsReservation reservation = new PointsReservation();
        reservation.setId(10L);
        reservation.setReservationNo("PR1");
        reservation.setOrderNo("ORD-1");
        reservation.setUserId(10001L);
        reservation.setReservedPoints(120);
        reservation.setConsumedPoints(120);
        reservation.setStatus("CONSUMED");
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsReservationMapper.updateById(org.mockito.ArgumentMatchers.<PointsReservation>any())).thenReturn(1);

        RefundCompensationResult result = service.compensate(new RefundCompensationRequest(
                "RF1", "ORD-1", 10001L, new BigDecimal("199.00"), "FULL", "refund:ORD-1:RF1"));

        assertThat(result.getRestoredPoints()).isEqualTo(120);
        verify(pointsService).reverseSpend(10001L, 120, "REFUND", "RF1",
                "REFUND:RF1:POINTS", "退款退回积分", "PR1", null);
        verify(growthService).add(eq(10001L), eq(-199), eq("REFUND"), eq("RF1"),
                eq("REFUND:RF1:GROWTH"), eq("退款扣回成长值"));
    }

    @Test
    void compensateShouldBeIdempotentByRefundRequestKey() {
        when(pointsTransactionMapper.selectCount(any())).thenReturn(1L);

        RefundCompensationResult result = service.compensate(new RefundCompensationRequest(
                "RF1", "ORD-1", 10001L, new BigDecimal("199.00"), "FULL", "refund:ORD-1:RF1"));

        assertThat(result.isDuplicate()).isTrue();
        verify(pointsService, never()).reverseSpend(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void compensateShouldRestorePartialRefundByPointsDeductionSnapshot() {
        PointsReservation reservation = new PointsReservation();
        reservation.setId(10L);
        reservation.setReservationNo("PR1");
        reservation.setOrderNo("ORD-1");
        reservation.setUserId(10001L);
        reservation.setReservedPoints(5000);
        reservation.setConsumedPoints(5000);
        reservation.setStatus("CONSUMED");
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsReservationMapper.updateById(org.mockito.ArgumentMatchers.<PointsReservation>any())).thenReturn(1);
        OrderMemberVO order = new OrderMemberVO();
        order.setOrderNo("ORD-1");
        order.setUserId(10001L);
        order.setOriginalAmount(new BigDecimal("199.00"));
        order.setTotalAmount(new BigDecimal("149.00"));
        order.setPointsUsed(5000);
        order.setPointsDeductionAmount(new BigDecimal("50.00"));
        order.setPointsDeductionRatio(100);
        when(orderClient.getOrderForMember("ORD-1")).thenReturn(Result.ok(order));

        RefundCompensationResult result = service.compensate(new RefundCompensationRequest(
                "RF2", "ORD-1", 10001L, new BigDecimal("50.00"), "PARTIAL", "refund:ORD-1:RF2"));

        assertThat(result.getRestoredPoints()).isEqualTo(5000);
        verify(pointsService).reverseSpend(10001L, 5000, "REFUND", "RF2",
                "REFUND:RF2:POINTS", "退款退回积分", "PR1", null);
    }
}
