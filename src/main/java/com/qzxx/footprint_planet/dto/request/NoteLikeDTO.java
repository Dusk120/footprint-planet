package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;

public record NoteLikeDTO(
        @NotNull(message = "like 不能为空")
        Boolean like
) {
}
