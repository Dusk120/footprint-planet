package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusDTO(
        @NotNull(message = "status 不能为空")
        Integer status
) {
}
