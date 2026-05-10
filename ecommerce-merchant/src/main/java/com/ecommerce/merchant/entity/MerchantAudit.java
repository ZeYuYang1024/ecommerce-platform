package com.ecommerce.merchant.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_audit")
public class MerchantAudit extends BaseEntity {
    private Long merchantId;
    private Long auditorId;
    private Integer action;
    private String comment;
}
