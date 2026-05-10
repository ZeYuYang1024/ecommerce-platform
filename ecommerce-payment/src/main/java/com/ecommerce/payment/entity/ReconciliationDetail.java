package com.ecommerce.payment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reconciliation_detail")
public class ReconciliationDetail extends BaseEntity {
    private Long reconciliationId;
    private String recordType;
    private String orderNo;
    private String paymentNo;
    private BigDecimal amount;
    private Integer recordStatus;
    private String matchStatus;
    private String diffReason;
}
