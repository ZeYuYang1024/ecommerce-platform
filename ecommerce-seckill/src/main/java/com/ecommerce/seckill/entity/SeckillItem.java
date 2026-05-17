package com.ecommerce.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_item")
public class SeckillItem extends BaseEntity {
    private Long merchantId;

    @NotNull(message = "sessionId is required")
    private Long sessionId;

    @NotNull(message = "spuId is required")
    private Long spuId;

    @NotNull(message = "skuId is required")
    private Long skuId;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "originalPrice is required")
    private BigDecimal originalPrice;

    @NotNull(message = "seckillPrice is required")
    private BigDecimal seckillPrice;

    @NotNull(message = "stockCount is required")
    private Integer stockCount;

    @NotNull(message = "remainingCount is required")
    private Integer remainingCount;
    private Integer status;
}
