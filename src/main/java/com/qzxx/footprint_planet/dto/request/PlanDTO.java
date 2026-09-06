package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PlanDTO(
        @NotBlank(message = "规划名称不能为空")
        String name,

        @NotBlank(message = "目的地不能为空")
        String destination,

        String location,

        String image,

        LocalDate startDate,

        Boolean isPublic,

        List<Map<String, Object>> days,

        List<Map<String, Object>> waypoints
) {
}
