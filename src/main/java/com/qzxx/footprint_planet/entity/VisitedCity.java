package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "visited_city", autoResultMap = true)
public class VisitedCity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String cityName;

    private String province;

    private String adcode;

    private Double lat;

    private Double lng;

    private String color;

    private LocalDate lightDate;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> records;

    private LocalDateTime createdAt;
}
