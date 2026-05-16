package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("brand")
public class Brand extends BaseEntity {
    private String name;
    private String logo;
    private String description;
    private Long merchantId;
    private String sourceType;
    private String auditStatus;
}
