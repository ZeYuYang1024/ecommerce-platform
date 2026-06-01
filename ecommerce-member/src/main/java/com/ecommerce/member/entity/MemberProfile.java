package com.ecommerce.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.ecommerce.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_profile")
public class MemberProfile extends BaseEntity {
    private Long userId;
    private Long levelId;
    private Long growthValue;
    private Long totalGrowthValue;
    private Long availablePoints;
    private Long totalEarnedPoints;
    private Long totalSpentPoints;

    @Version
    private Long version;
}
