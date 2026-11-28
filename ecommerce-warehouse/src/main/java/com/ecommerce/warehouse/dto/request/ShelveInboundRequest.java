package com.ecommerce.warehouse.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ShelveInboundRequest(@NotEmpty List<@Valid @NotNull Placement> placements) {
    public record Placement(@NotNull Long itemId, @NotNull Long binId) {}
}
