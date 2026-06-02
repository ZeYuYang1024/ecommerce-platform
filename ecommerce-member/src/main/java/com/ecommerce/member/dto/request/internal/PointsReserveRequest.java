package com.ecommerce.member.dto.request.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsReserveRequest {
    private Long userId;
    private String orderNo;
    private String sceneType;
    private Integer points;
    private String idempotencyKey;
}
