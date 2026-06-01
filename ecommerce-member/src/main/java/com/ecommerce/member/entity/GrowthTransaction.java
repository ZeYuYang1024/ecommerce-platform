package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("growth_transaction")
public class GrowthTransaction {
    private Long id;
    private Long userId;
    private Integer amount;
    private Long balanceAfter;
    private String sourceType;
    private String sourceId;
    private String bizKey;
    private String remark;
    private LocalDateTime createdAt;
}
