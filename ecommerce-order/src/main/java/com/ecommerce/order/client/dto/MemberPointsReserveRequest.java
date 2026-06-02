package com.ecommerce.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberPointsReserveRequest {
    private Long userId;
    private String orderNo;
    private String sceneType;
    private Integer points;
    private String idempotencyKey;
}
