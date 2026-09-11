package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "feedback", autoResultMap = true)
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    private String contact;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    private Boolean handled;

    private LocalDateTime createdAt;
}
