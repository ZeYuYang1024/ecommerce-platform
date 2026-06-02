package com.ecommerce.member.controller.internal;

import com.ecommerce.common.result.Result;
import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.dto.request.internal.PointsReserveRequest;
import com.ecommerce.member.dto.request.internal.RefundCompensationRequest;
import com.ecommerce.member.dto.response.internal.PointsReserveResponse;
import com.ecommerce.member.dto.response.internal.RefundCompensationResult;
import com.ecommerce.member.service.PointsReservationService;
import com.ecommerce.member.service.RefundCompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/member")
public class MemberInternalController {

    private final PointsReservationService pointsReservationService;
    private final RefundCompensationService refundCompensationService;

    @PostMapping("/points/reservations")
    public Result<PointsReserveResponse> reserve(@RequestBody PointsReserveRequest request) {
        return Result.ok(pointsReservationService.reserve(request));
    }

    @PostMapping("/points/reservations/{reservationNo}/confirm")
    public Result<Void> confirm(@PathVariable String reservationNo,
                                @RequestBody PointsReservationConfirmRequest request) {
        request.setReservationNo(reservationNo);
        pointsReservationService.confirm(request);
        return Result.ok();
    }

    @PostMapping("/points/reservations/{reservationNo}/release")
    public Result<Void> release(@PathVariable String reservationNo,
                                @RequestBody PointsReservationReleaseRequest request) {
        request.setReservationNo(reservationNo);
        pointsReservationService.release(request);
        return Result.ok();
    }

    @PostMapping("/refund-compensations")
    public Result<RefundCompensationResult> compensate(@RequestBody RefundCompensationRequest request) {
        return Result.ok(refundCompensationService.compensate(request));
    }
}
