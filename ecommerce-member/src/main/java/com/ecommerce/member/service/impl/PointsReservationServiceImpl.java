package com.ecommerce.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.result.BusinessException;
import com.ecommerce.common.util.SnowflakeUtils;
import com.ecommerce.member.common.MemberErrorCode;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.dto.request.internal.PointsReserveRequest;
import com.ecommerce.member.dto.response.internal.PointsReserveResponse;
import com.ecommerce.member.entity.MemberProfile;
import com.ecommerce.member.entity.PointsReservation;
import com.ecommerce.member.mapper.MemberProfileMapper;
import com.ecommerce.member.mapper.PointsReservationMapper;
import com.ecommerce.member.service.PointsReservationService;
import com.ecommerce.member.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PointsReservationServiceImpl implements PointsReservationService {

    private final PointsReservationMapper pointsReservationMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final PointsService pointsService;

    @Override
    @Transactional
    public PointsReserveResponse reserve(PointsReserveRequest request) {
        PointsReservation existing = pointsReservationMapper.selectOne(
                new LambdaQueryWrapper<PointsReservation>()
                        .eq(PointsReservation::getIdempotencyKey, request.getIdempotencyKey()));
        if (existing != null) {
            if (!java.util.Objects.equals(existing.getUserId(), request.getUserId())
                    || !java.util.Objects.equals(existing.getOrderNo(), request.getOrderNo())
                    || !java.util.Objects.equals(existing.getReservedPoints(), request.getPoints())
                    || !"RESERVED".equals(existing.getStatus())
                    || (existing.getExpiredAt() != null && !existing.getExpiredAt().isAfter(LocalDateTime.now()))) {
                throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_STATUS_INVALID);
            }
            return toReserveResponse(existing);
        }

        if (request.getPoints() == null || request.getPoints() <= 0) {
            throw new BusinessException(MemberErrorCode.INVALID_POINTS_AMOUNT);
        }

        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, request.getUserId()));
        if (profile == null || profile.getAvailablePoints() == null || profile.getAvailablePoints() < request.getPoints()) {
            throw new BusinessException(MemberErrorCode.POINTS_INSUFFICIENT);
        }

        int rows = memberProfileMapper.update(null,
                new LambdaUpdateWrapper<MemberProfile>()
                        .eq(MemberProfile::getId, profile.getId())
                        .eq(MemberProfile::getVersion, profile.getVersion())
                        .setSql("available_points = available_points - " + request.getPoints())
                        .setSql("version = version + 1"));
        if (rows == 0) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_CONFLICT);
        }

        PointsReservation reservation = new PointsReservation();
        reservation.setId(SnowflakeUtils.nextId());
        reservation.setReservationNo("PR" + SnowflakeUtils.nextId());
        reservation.setUserId(request.getUserId());
        reservation.setOrderNo(request.getOrderNo());
        reservation.setSceneType(request.getSceneType());
        reservation.setReservedPoints(request.getPoints());
        reservation.setExpiredAt(LocalDateTime.now().plusHours(1));
        reservation.setConsumedPoints(0);
        reservation.setReleasedPoints(0);
        reservation.setStatus("RESERVED");
        reservation.setIdempotencyKey(request.getIdempotencyKey());
        pointsReservationMapper.insert(reservation);
        pointsService.holdEarnLots(request.getUserId(), request.getPoints(), reservation.getReservationNo());
        return toReserveResponse(reservation);
    }

    @Override
    @Transactional
    public void confirm(PointsReservationConfirmRequest request) {
        PointsReservation reservation = getReservation(request.getReservationNo());
        if ("CONSUMED".equals(reservation.getStatus())) {
            return;
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_STATUS_INVALID);
        }

        int claimRows = pointsReservationMapper.update(null,
                new LambdaUpdateWrapper<PointsReservation>()
                        .eq(PointsReservation::getId, reservation.getId())
                        .eq(PointsReservation::getStatus, "RESERVED")
                        .setSql("consumed_points = " + reservation.getReservedPoints())
                        .setSql("status = 'CONSUMED'"));
        if (claimRows == 0) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_CONFLICT);
        }

        pointsService.spend(reservation.getUserId(), reservation.getReservedPoints(), "ORDER",
                reservation.getOrderNo(), resolveConfirmBizKey(request, reservation),
                "订单积分抵扣", reservation.getReservationNo());
    }

    @Override
    @Transactional
    public void release(PointsReservationReleaseRequest request) {
        PointsReservation reservation = getReservation(request.getReservationNo());
        if ("RELEASED".equals(reservation.getStatus())) {
            return;
        }
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_STATUS_INVALID);
        }

        int claimRows = pointsReservationMapper.update(null,
                new LambdaUpdateWrapper<PointsReservation>()
                        .eq(PointsReservation::getId, reservation.getId())
                        .eq(PointsReservation::getStatus, "RESERVED")
                        .setSql("status = 'RELEASED'"));
        if (claimRows == 0) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_CONFLICT);
        }

        MemberProfile profile = memberProfileMapper.selectOne(
                new LambdaQueryWrapper<MemberProfile>()
                        .eq(MemberProfile::getUserId, reservation.getUserId()).last("FOR UPDATE"));
        if (profile == null) {
            throw new BusinessException(MemberErrorCode.MEMBER_PROFILE_NOT_FOUND);
        }

        int releasable = pointsService.releaseEarnLots(reservation.getId(), reservation.getReservedPoints());
        int rows = memberProfileMapper.update(null,
                new LambdaUpdateWrapper<MemberProfile>()
                        .eq(MemberProfile::getId, profile.getId())
                        .eq(MemberProfile::getVersion, profile.getVersion())
                        .setSql("available_points = available_points + " + releasable)
                        .setSql("version = version + 1"));
        if (rows == 0) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_CONFLICT);
        }
        pointsReservationMapper.update(null, new LambdaUpdateWrapper<PointsReservation>()
                .eq(PointsReservation::getId, reservation.getId()).setSql("released_points = " + releasable));
    }

    private PointsReservation getReservation(String reservationNo) {
        PointsReservation reservation = pointsReservationMapper.selectOne(
                new LambdaQueryWrapper<PointsReservation>()
                        .eq(PointsReservation::getReservationNo, reservationNo));
        if (reservation == null) {
            throw new BusinessException(MemberErrorCode.POINTS_RESERVATION_NOT_FOUND);
        }
        return reservation;
    }

    private PointsReserveResponse toReserveResponse(PointsReservation reservation) {
        PointsReserveResponse response = new PointsReserveResponse();
        response.setReservationNo(reservation.getReservationNo());
        response.setReservedPoints(reservation.getReservedPoints());
        response.setStatus(reservation.getStatus());
        return response;
    }

    private String resolveConfirmBizKey(PointsReservationConfirmRequest request, PointsReservation reservation) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return request.getIdempotencyKey();
        }
        return "ORDER:" + reservation.getOrderNo() + ":SPEND";
    }
}
