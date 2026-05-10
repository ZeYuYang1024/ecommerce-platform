package com.ecommerce.payment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reconciliation")
public class Reconciliation extends BaseEntity {
    private String batchNo;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer totalOrderCount;
    private Integer totalPaymentCount;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private Integer status;
}
