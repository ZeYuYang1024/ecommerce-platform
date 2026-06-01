package com.ecommerce.member.dto.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MemberLevelVO {
    private Long id;
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
