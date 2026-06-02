package com.ecommerce.member.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PointsTransactionVO {
    private Long id;
    private String direction;
    private Integer amount;
    private Long balanceAfter;
    private String sourceType;
    private String sourceId;
    private String remark;
    private LocalDateTime expireAt;
    private LocalDateTime createdAt;
}
