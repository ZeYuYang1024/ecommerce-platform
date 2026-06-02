package com.ecommerce.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPointsReservationReleaseRequest {
    private String reservationNo;
    private String reason;
    private String idempotencyKey;
}
