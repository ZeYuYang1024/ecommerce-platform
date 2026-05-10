package com.ecommerce.payment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("daily_settlement")
public class DailySettlement extends BaseEntity {
    private LocalDate settlementDate;
    private Integer totalOrderCount;
    private BigDecimal totalOrderAmount;
    private Integer totalPaymentCount;
    private BigDecimal totalPaymentAmount;
    private Integer totalRefundCount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netAmount;
    private Integer status;
}
