package com.ecommerce.member.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PointsGrantRequest {
    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "amount is required")
    @Min(value = 1, message = "amount must be positive")
    private Integer amount;

    @NotBlank(message = "sourceId is required")
    private String sourceId;

    private String remark;
}
