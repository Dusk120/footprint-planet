package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_collaborator")
public class PlanCollaborator {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long userId;

    /** editor / viewer（owner 由 plan.user_id 判定） */
    private String role;

    private LocalDateTime joinedAt;
}
