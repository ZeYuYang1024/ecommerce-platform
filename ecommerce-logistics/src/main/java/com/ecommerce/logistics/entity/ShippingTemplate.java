package com.ecommerce.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shipping_template")
public class ShippingTemplate extends BaseEntity {
    private String templateName;
    private Long merchantId;
    private Integer calcType;
    private Integer firstUnit;
    private BigDecimal firstFee;
    private Integer continueUnit;
    private BigDecimal continueFee;
    private String freeCondition;
    private String regionRules;
}
