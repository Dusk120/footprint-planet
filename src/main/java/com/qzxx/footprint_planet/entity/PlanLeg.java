package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_leg")
public class PlanLeg {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long fromItemId;

    private Long toItemId;

    /** driving / walking / transit */
    private String mode;

    /** 时长（秒） */
    private Integer duration;

    /** 距离（米） */
    private Integer distance;

    /** 方案摘要 */
    private String summary;

    /** 简化路线坐标串（画线用，压缩存储） */
    private String polyline;

    private LocalDateTime createdAt;
}
