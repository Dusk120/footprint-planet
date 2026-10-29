package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlanInviteDTO {

    /** 进入后的角色：editor / viewer */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 过期时间（可空 = 不过期） */
    private LocalDateTime expiresAt;

    /** 次数上限（0 = 不限） */
    private Integer maxUses;
}
