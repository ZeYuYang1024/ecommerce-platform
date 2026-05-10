package com.ecommerce.merchant.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant")
public class Merchant extends BaseEntity {
    private String name;
    private String logo;
    private String contactName;
    private String contactPhone;
    private String businessLicense;
    private Integer status;
    private String reason;
}
