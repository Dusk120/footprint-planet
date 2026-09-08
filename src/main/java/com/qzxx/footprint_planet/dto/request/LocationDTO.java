package com.qzxx.footprint_planet.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO {

    private String name;

    private String province;

    private String adcode;

    private Double lng;

    private Double lat;
}
