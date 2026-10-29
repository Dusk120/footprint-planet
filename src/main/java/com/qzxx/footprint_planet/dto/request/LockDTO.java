package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LockDTO {

    @NotNull(message = "locked 不能为空")
    private Boolean locked;
}
