package com.ecommerce.member.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GrowthTransactionVO {
    private Long id;
    private Integer amount;
    private Long balanceAfter;
    private String sourceType;
    private String sourceId;
    private String remark;
    private LocalDateTime createdAt;
}
