package com.ecommerce.order.client.dto;

import lombok.Data;

@Data
public class MemberPointsReserveResponse {
    private String reservationNo;
    private Integer reservedPoints;
    private String status;
}
