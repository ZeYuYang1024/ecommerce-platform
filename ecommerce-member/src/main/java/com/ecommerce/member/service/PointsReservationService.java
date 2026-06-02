package com.ecommerce.member.service;

import com.ecommerce.member.dto.request.internal.PointsReservationConfirmRequest;
import com.ecommerce.member.dto.request.internal.PointsReservationReleaseRequest;
import com.ecommerce.member.dto.request.internal.PointsReserveRequest;
import com.ecommerce.member.dto.response.internal.PointsReserveResponse;

public interface PointsReservationService {

    PointsReserveResponse reserve(PointsReserveRequest request);

    void confirm(PointsReservationConfirmRequest request);

    void release(PointsReservationReleaseRequest request);
}
