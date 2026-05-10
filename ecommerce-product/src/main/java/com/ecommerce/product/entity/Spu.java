package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spu")
public class Spu extends BaseEntity {
    private String name;
    private Long categoryId;
    private Long brandId;
    private Long merchantId;
    private String description;
    private String mainImage;
    private String images;
    private String detail;
    private Integer status;
    private BigDecimal avgRating;
    private Integer reviewCount;
}
