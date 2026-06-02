package com.ecommerce.member.dto.response.internal;

import lombok.Data;

@Data
public class PointsReserveResponse {
    private String reservationNo;
    private Integer reservedPoints;
    private String status;
}
