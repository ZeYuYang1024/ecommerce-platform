package com.ecommerce.product.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class Review extends BaseEntity {
    private Long spuId;
    private Long userId;
    private String username;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
}
