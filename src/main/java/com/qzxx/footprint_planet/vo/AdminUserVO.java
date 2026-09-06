package com.qzxx.footprint_planet.vo;

import java.time.LocalDateTime;

/** 管理端用户（列表 / 详情共用字段） */
public record AdminUserVO(
        Long id,
        String username,
        String nickname,
        String role,
        Integer status,
        String motto,
        Integer level,
        LocalDateTime createdAt,
        Integer visitedCount,
        Integer noteCount
) {
}
