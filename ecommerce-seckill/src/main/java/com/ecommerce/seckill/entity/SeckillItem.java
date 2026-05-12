package com.ecommerce.seckill.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_item")
public class SeckillItem extends BaseEntity {
    private Long sessionId;
    private Long spuId;
    private Long skuId;
    private String name;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private Integer remainingCount;
    private Integer status;
}
