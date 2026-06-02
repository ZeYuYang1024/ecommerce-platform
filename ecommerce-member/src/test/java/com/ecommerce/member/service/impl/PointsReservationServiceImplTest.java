package com.ecommerce.member.service.impl;

import com.ecommerce.common.result.BusinessException;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.dto.request.internal.PointsReserveRequest;
import com.ecommerce.member.dto.response.internal.PointsReserveResponse;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsConsumeDetailMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.mapper.PointsTransactionMapper;
import com.ecommerce.member.service.PointsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.<PointsReservation>argThat;
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
        verify(pointsReservationMapper).insert(argThat(reservation ->
                reservation.getUserId().equals(10001L)
                        && "ORD-1".equals(reservation.getOrderNo())
                        && "ORDER_DEDUCTION".equals(reservation.getSceneType())
                        && Integer.valueOf(120).equals(reservation.getReservedPoints())
                        && Integer.valueOf(0).equals(reservation.getConsumedPoints())
                        && Integer.valueOf(0).equals(reservation.getReleasedPoints())
                        && "RESERVED".equals(reservation.getStatus())
                        && "reserve:ORD-1".equals(reservation.getIdempotencyKey())));
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
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(pointsReservationMapper.updateById(any(PointsReservation.class))).thenReturn(1);

        service.confirm(new PointsReservationConfirmRequest("PR1", "confirm:ORD-1"));

        verify(pointsService).spend(10001L, 120, "ORDER", "ORD-1",
                "ORDER:ORD-1:SPEND", "订单积分抵扣", "PR1");
        verify(pointsReservationMapper).updateById(argThat(updated ->
                updated.getId().equals(reservation.getId())
                        && Integer.valueOf(120).equals(updated.getConsumedPoints())
                        && Integer.valueOf(0).equals(updated.getReleasedPoints())
                        && "CONSUMED".equals(updated.getStatus())));
        verify(pointsConsumeDetailMapper, never()).insert(any());
    }

    @Test
    void releaseShouldMarkReservationReleasedAndReturnPointsToProfile() {
        PointsReservation reservation = buildReservation("PR1", "ORD-1", 10001L, 120, "RESERVED");
        MemberProfile profile = buildProfile(1L, 10001L, 380L, 2L);
        when(pointsReservationMapper.selectOne(any())).thenReturn(reservation);
        when(memberProfileMapper.selectOne(any())).thenReturn(profile);
        when(memberProfileMapper.update(any(), any())).thenReturn(1);
        when(pointsReservationMapper.updateById(any(PointsReservation.class))).thenReturn(1);

        service.release(new PointsReservationReleaseRequest("PR1", "USER_CANCELLED", "release:ORD-1"));

        verify(memberProfileMapper).update(any(), any());
        verify(pointsReservationMapper).updateById(argThat(updated ->
                updated.getId().equals(reservation.getId())
                        && Integer.valueOf(0).equals(updated.getConsumedPoints())
                        && Integer.valueOf(120).equals(updated.getReleasedPoints())
                        && "RELEASED".equals(updated.getStatus())));
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
