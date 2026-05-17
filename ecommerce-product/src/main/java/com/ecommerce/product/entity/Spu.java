package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("spu")
public class Spu extends BaseEntity {
    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "categoryId is required")
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
