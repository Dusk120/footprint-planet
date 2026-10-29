package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    @NotBlank(message = "规划名称不能为空")
    private String name;

    @NotBlank(message = "目的地不能为空")
    private String destination;

    private String location;

    private String image;

    private LocalDate startDate;

    private Boolean isPublic;
}
