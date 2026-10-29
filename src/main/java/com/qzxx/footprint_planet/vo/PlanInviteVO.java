package com.qzxx.footprint_planet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanInviteVO {

    private Long id;

    private String token;

    /** 完整邀请链接（后端拼好，前端直接用） */
    private String link;

    /** 进入后的角色：editor / viewer */
    private String role;

    private LocalDateTime expiresAt;

    private Integer maxUses;

    private Integer usedCount;

    private Boolean revoked;

    private LocalDateTime createdAt;
}
