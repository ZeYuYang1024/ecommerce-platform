package com.ecommerce.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {
    private Long userId;
    private Long templateId;
    private Integer status;
    private String orderNo;
    private LocalDateTime usedAt;
}
