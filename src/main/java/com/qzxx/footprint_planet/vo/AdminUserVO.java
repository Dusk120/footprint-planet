package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 管理端用户（列表 / 详情共用字段） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String role;

    private Integer status;

    private String motto;

    private LocalDateTime createdAt;

    private Integer visitedCount;

    private Integer noteCount;
}
