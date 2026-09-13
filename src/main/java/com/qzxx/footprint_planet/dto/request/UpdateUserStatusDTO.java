package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusDTO {

    @NotNull(message = "status 不能为空")
    @Min(0) @Max(1)
    private Integer status;
}
