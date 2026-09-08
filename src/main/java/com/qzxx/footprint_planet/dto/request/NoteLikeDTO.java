package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteLikeDTO {

    @NotNull(message = "like 不能为空")
    private Boolean like;
}
