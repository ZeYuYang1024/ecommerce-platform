package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_reservation")
public class PointsReservation {
    private Long id;
    private String reservationNo;
    private Long userId;
    private String orderNo;
    private String sceneType;
    private Integer reservedPoints;
    private Integer consumedPoints;
    private Integer releasedPoints;
    private String status;
    private String idempotencyKey;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
