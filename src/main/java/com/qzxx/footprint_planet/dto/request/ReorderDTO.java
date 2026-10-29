package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReorderDTO {

    @Valid
    @NotNull(message = "天数排布不能为空")
    @Size(min = 1, message = "至少包含一天")
    private List<DayOrderDTO> days;

    @NotNull(message = "版本号不能为空")
    private Integer version;
}
