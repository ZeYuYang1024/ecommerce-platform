package com.ecommerce.member.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.dto.request.internal.PointsReserveRequest;
import com.ecommerce.member.dto.response.internal.PointsReserveResponse;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.entity.PointsTransaction;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsConsumeDetailMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsReservationServiceImplTest {

    @Mock
    private PointsReservationMapper pointsReservationMapper;

    @Mock
    private PointsConsumeDetailMapper pointsConsumeDetailMapper;

    @Mock
    private PointsTransactionMapper pointsTransactionMapper;

    @Mock
    private MemberProfileMapper memberProfileMapper;

    @Mock
    private PointsService pointsService;

    @InjectMocks
    private PointsReservationServiceImpl service;

    @Test
    void reserveShouldCreateReservationWhenBalanceEnough() {
        MemberProfile profile = buildProfile(1L, 10001L, 500L, 0L);
        when(pointsReservationMapper.selectOne(any())).thenReturn(null);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);

        PointsReserveResponse result = service.reserve(new PointsReserveRequest(
                10001L, "ORD-1", "ORDER_DEDUCTION", 120, "reserve:ORD-1"));

        assertThat(result.getReservedPoints()).isEqualTo(120);
        assertThat(result.getStatus()).isEqualTo("RESERVED");
        assertThat(result.getReservationNo()).startsWith("PR");
        ArgumentCaptor<PointsReservation> reservationCaptor = ArgumentCaptor.forClass(PointsReservation.class);
        verify(pointsReservationMapper).insert(reservationCaptor.capture());
        PointsReservation inserted = reservationCaptor.getValue();
        assertThat(inserted.getUserId()).isEqualTo(10001L);
        assertThat(inserted.getOrderNo()).isEqualTo("ORD-1");
        assertThat(inserted.getSceneType()).isEqualTo("ORDER_DEDUCTION");
        assertThat(inserted.getReservedPoints()).isEqualTo(120);
        assertThat(inserted.getConsumedPoints()).isEqualTo(0);
        assertThat(inserted.getReleasedPoints()).isEqualTo(0);
        assertThat(inserted.getStatus()).isEqualTo("RESERVED");
        assertThat(inserted.getIdempotencyKey()).isEqualTo("reserve:ORD-1");
    }

    @Test
    void reserveShouldRejectWhenPointsInsufficient() {
        MemberProfile profile = buildProfile(1L, 10001L, 80L, 0L);
        when(pointsReservationMapper.selectOne(any())).thenReturn(null);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);

        assertThatThrownBy(() -> service.reserve(new PointsReserveRequest(
                10001L, "ORD-1", "ORDER_DEDUCTION", 120, "reserve:ORD-1")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(MemberErrorCode.POINTS_INSUFFICIENT);

        verify(memberProfileMapper, never()).update(any(), any());
        verify(pointsReservationMapper, never()).insert(any(PointsReservation.class));
    }

    @Test
    void confirmShouldCreateSpendLedgerAndMarkReservationConsumed() {
        PointsReservation reservation = buildReservation("PR1", "ORD-1", 10001L, 120, "RESERVED");
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 2L);
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsReservationMapper.update(any(), any())).thenReturn(1);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);

        service.confirm(new PointsReservationConfirmRequest("PR1", "confirm:ORD-1"));

        verify(pointsService, never()).spend(any(), any(), any(), any(), any(), any(), any());
        verify(pointsReservationMapper).update(any(), any());
        verify(memberProfileMapper).update(any(), any());
        ArgumentCaptor<PointsTransaction> spendCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
        verify(pointsTransactionMapper).insert(spendCaptor.capture());
        PointsTransaction spend = spendCaptor.getValue();
        assertThat(spend.getUserId()).isEqualTo(10001L);
        assertThat(spend.getDirection()).isEqualTo("SPEND");
        assertThat(spend.getAmount()).isEqualTo(120);
        assertThat(spend.getBalanceAfter()).isEqualTo(380L);
        assertThat(spend.getSourceType()).isEqualTo("ORDER");
        assertThat(spend.getSourceId()).isEqualTo("ORD-1");
        assertThat(spend.getBizKey()).isEqualTo("confirm:ORD-1");
        assertThat(spend.getRemark()).isEqualTo("订单积分抵扣");
        assertThat(spend.getRelatedReservationNo()).isEqualTo("PR1");
        verify(pointsConsumeDetailMapper, never()).insert(any(com.ecommerce.member.entity.PointsConsumeDetail.class));
    }

    @Test
    void releaseShouldMarkReservationReleasedAndReturnPointsToProfile() {
        PointsReservation reservation = buildReservation("PR1", "ORD-1", 10001L, 120, "RESERVED");
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 2L);
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsReservationMapper.update(any(), any())).thenReturn(1);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);

        service.release(new PointsReservationReleaseRequest("PR1", "USER_CANCELLED", "release:ORD-1"));

        verify(memberProfileMapper).update(any(), any());
        verify(pointsReservationMapper).update(any(), any());
    }

    private MemberProfile buildProfile(Long id, Long userId, Long availablePoints, Long version) {
        MemberProfile profile = new MemberProfile();
        profile.setId(id);
        profile.setUserId(userId);
        profile.setAvailablePoints(availablePoints);
        profile.setVersion(version);
        return profile;
    }

    private PointsReservation buildReservation(String reservationNo, String orderNo, Long userId,
                                               Integer reservedPoints, String status) {
        PointsReservation reservation = new PointsReservation();
        reservation.setId(10L);
        reservation.setReservationNo(reservationNo);
        reservation.setUserId(userId);
        reservation.setOrderNo(orderNo);
        reservation.setSceneType("ORDER_DEDUCTION");
        reservation.setReservedPoints(reservedPoints);
        reservation.setConsumedPoints(0);
        reservation.setReleasedPoints(0);
        reservation.setStatus(status);
        reservation.setIdempotencyKey("reserve:" + orderNo);
        return reservation;
    }
}
