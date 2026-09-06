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
@TableName(value = "plan", autoResultMap = true)
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String destination;

    private String location;

    private String image;

    private LocalDate startDate;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> days;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> waypoints;

    private Boolean isPublic;

    private LocalDateTime createdAt;
}
