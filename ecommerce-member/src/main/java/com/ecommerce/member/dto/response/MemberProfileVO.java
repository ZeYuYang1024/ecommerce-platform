package com.ecommerce.member.dto.response;

import lombok.Data;

@Data
public class MemberProfileVO {
    private Long userId;
    private MemberLevelVO level;
    private Long growthValue;
    private Long totalGrowthValue;
    private Long nextLevelGrowth;
    private Long availablePoints;
    private Long totalEarnedPoints;
    private Long totalSpentPoints;
}
