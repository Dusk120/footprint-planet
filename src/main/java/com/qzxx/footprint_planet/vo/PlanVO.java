package com.qzxx.footprint_planet.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 规划对象；author 为作者主页公开规划场景附加的只读昵称字段 */
public record PlanVO(
        Long id,
        Long userId,
        String name,
        String destination,
        String location,
        String image,
        LocalDate startDate,
        Boolean isPublic,
        List<Map<String, Object>> days,
        List<Map<String, Object>> waypoints,
        String author,
        LocalDateTime createdAt
) {
}
