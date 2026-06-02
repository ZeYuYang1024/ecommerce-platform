package com.ecommerce.order.client;

import com.ecommerce.common.result.Result;
import com.ecommerce.order.client.dto.MemberPointsReservationReleaseRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveRequest;
import com.ecommerce.order.client.dto.MemberPointsReserveResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecommerce-member")
public interface MemberClient {

    @PostMapping("/api/v1/internal/member/points/reservations")
    Result<MemberPointsReserveResponse> reservePoints(@RequestBody MemberPointsReserveRequest request);

    @PostMapping("/api/v1/internal/member/points/reservations/{reservationNo}/release")
    Result<Void> releasePoints(@PathVariable("reservationNo") String reservationNo,
                               @RequestBody MemberPointsReservationReleaseRequest request);
}
