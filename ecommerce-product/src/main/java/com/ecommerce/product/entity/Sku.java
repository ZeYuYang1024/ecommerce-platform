package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sku")
public class Sku extends BaseEntity {
    private Long spuId;
    private String name;
    private String spec;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String image;
}
