package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("points_transaction")
public class PointsTransaction {
    private Long id;
    private Long userId;
    private String direction;
    private Integer amount;
    private Long balanceAfter;
    private String sourceType;
    private String sourceId;
    private String bizKey;
    private Integer consumedAmount;
    private LocalDateTime expireAt;
    private String remark;
    private String relatedReservationNo;
    private Long reversalOfTxId;
    private LocalDateTime createdAt;
}
