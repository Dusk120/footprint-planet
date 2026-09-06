package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record VisitedCityDTO(
        @NotBlank(message = "城市名不能为空")
        String cityName,

        @NotBlank(message = "省份不能为空")
        String province,

        String adcode,

        Double lat,

        Double lng,

        String color,

        LocalDate lightDate,

        List<Map<String, Object>> records
) {
}
