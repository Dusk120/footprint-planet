package com.qzxx.footprint_planet.vo;

public record UserVO(
        Long id,
        String username,
        String nickname,
        String avatar,
        String motto,
        Integer level,
        String levelTitle,
        Integer visitedCities,
        Integer visitedProvinces,
        Integer totalKm,
        Integer totalDays,
        String role
) {
}
