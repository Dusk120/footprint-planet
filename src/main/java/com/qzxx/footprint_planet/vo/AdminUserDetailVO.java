package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailVO {

    private Long id;

    private String username;

    private String nickname;

    private String role;

    private Integer status;

    private String motto;

    private LocalDateTime createdAt;

    private Integer visitedCount;

    private Integer noteCount;

    private String avatar;

    private Integer totalKm;

    private Integer totalDays;

    private Integer visitedProvinces;
}
