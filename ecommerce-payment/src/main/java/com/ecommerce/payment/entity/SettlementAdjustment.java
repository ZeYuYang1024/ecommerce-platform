package com.ecommerce.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("settlement_adjustment")
public class SettlementAdjustment {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long settlementId;
    private int totalPaymentCount;
    private BigDecimal totalPaymentAmount;
    private int totalRefundCount;
    private BigDecimal totalRefundAmount;
    private LocalDateTime createdAt;
}
