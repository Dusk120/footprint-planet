package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("feedback")
public class Feedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String content;

    private String contact;

    /** 处理状态：true 已处理 / false 待处理 */
    private Boolean handled;

    private LocalDateTime createdAt;
}
