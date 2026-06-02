package com.ecommerce.member.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LevelUpdateRequest {
    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "growthThreshold is required")
    @Min(value = 0, message = "growthThreshold must be >= 0")
    private Long growthThreshold;

    @NotNull(message = "pointsMultiplier is required")
    private BigDecimal pointsMultiplier;

    private Integer birthdayGiftPoints;
    private BigDecimal discountRate;
    private Integer freeShipping;
    private Integer prioritySupport;
    private Integer earlyAccess;
    private String iconUrl;
    private String description;
}
