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
@TableName(value = "note", autoResultMap = true)
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;

    private String authorNickname;

    private String title;

    private String content;

    private String image;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photos;

    private String locationName;

    private String locationProvince;

    private Integer likes;

    private LocalDateTime createdAt;
}
