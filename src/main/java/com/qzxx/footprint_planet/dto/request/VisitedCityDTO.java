package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitedCityDTO {

    @NotBlank(message = "城市名不能为空")
    private String cityName;

    @NotBlank(message = "省份不能为空")
    private String province;

    private String adcode;

    private Double lat;

    private Double lng;

    private String color;

    private LocalDate lightDate;

    private List<Map<String, Object>> records;
}
