package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_invite")
public class PlanInvite {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 随机串（UUID），链接主体 */
    private String token;

    private Long planId;

    /** 进入后的角色：editor / viewer */
    private String role;

    /** 过期时间（可空 = 不过期） */
    private LocalDateTime expiresAt;

    /** 次数上限（0 = 不限） */
    private Integer maxUses;

    /** 已用次数 */
    private Integer usedCount;

    /** 邀请人 */
    private Long createdBy;

    /** 是否已撤销 */
    private Boolean revoked;

    private LocalDateTime createdAt;
}
