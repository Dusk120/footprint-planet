package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 更新邀请链接的字段（token 不变，链接不变） */
@Data
public class PlanInviteUpdateDTO {

    /** 进入后的角色：editor / viewer */
    @NotBlank(message = "角色不能为空")
    private String role;

    /** 过期时间（null = 不过期） */
    private LocalDateTime expiresAt;

    /** 次数上限（0 = 不限） */
    @NotNull(message = "次数上限不能为空")
    private Integer maxUses;
}
