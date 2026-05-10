package com.ecommerce.inventory.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock")
public class Stock extends BaseEntity {
    private Long skuId;
    private Integer totalStock;
    private Integer lockedStock;
    private Integer availableStock;
    private Integer version;
}
