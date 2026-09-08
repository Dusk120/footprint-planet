package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String motto;

    private Integer visitedCities;

    private Integer visitedProvinces;

    private Integer totalKm;

    private Integer totalDays;

    private String role;
}
