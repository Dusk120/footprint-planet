package com.qzxx.footprint_planet.dto.request;

public record LocationDTO(
        String name,

        String province,

        String adcode,

        Double lng,

        Double lat
) {
}
