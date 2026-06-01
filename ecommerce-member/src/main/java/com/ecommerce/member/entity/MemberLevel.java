package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_level")
public class MemberLevel extends BaseEntity {
    private String name;
    private String levelCode;
    private Integer sortOrder;
    private Long growthThreshold;
    private BigDecimal pointsMultiplier;
    private Integer birthdayGiftPoints;
    private BigDecimal discountRate;
    private Integer freeShipping;
    private Integer prioritySupport;
    private Integer earlyAccess;
    private String iconUrl;
    private String description;
}
