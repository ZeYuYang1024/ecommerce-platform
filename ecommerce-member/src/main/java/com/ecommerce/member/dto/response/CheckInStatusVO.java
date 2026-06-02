package com.ecommerce.member.dto.response;

import lombok.Data;

@Data
public class CheckInStatusVO {
    private Boolean checkedToday;
    private Integer consecutiveDays;
    private Integer pointsAwardedToday;
}
