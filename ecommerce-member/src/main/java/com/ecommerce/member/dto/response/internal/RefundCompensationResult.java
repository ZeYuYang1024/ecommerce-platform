package com.ecommerce.member.dto.response.internal;

import lombok.Data;

@Data
public class RefundCompensationResult {
    private boolean duplicate;
    private Integer restoredPoints;
    private Integer reversedGrowth;
}
