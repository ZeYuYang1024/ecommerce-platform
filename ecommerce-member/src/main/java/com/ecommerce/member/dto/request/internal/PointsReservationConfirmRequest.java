package com.ecommerce.member.dto.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsReservationConfirmRequest {
    private String reservationNo;
    private String idempotencyKey;
}
