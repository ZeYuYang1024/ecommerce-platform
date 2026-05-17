package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("brand")
public class Brand extends BaseEntity {
    @NotBlank(message = "name is required")
    private String name;
    private String logo;
    private String description;
    private Long merchantId;
    private String sourceType;
    private String auditStatus;
}
