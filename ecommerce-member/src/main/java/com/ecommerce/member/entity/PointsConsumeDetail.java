package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_consume_detail")
public class PointsConsumeDetail {
    private Long id;
    private Long reservationId;
    private Long userId;
    private Long earnTxId;
    private Integer consumePoints;
    private Integer restoredPoints;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
