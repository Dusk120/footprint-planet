package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 规划对象；author 为作者主页公开规划场景附加的只读昵称字段 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanVO {

    private Long id;

    private Long userId;

    private String name;

    private String destination;

    private String location;

    private String image;

    private LocalDate startDate;

    private Boolean isPublic;

    private List<Map<String, Object>> days;

    private List<Map<String, Object>> waypoints;

    private String author;

    private LocalDateTime createdAt;
}
