package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String motto;

    private Integer level;

    private String levelTitle;

    private Integer totalKm;

    private Integer totalDays;

    /** 角色：ADMIN / USER（默认 USER） */
    private String role;

    /** 状态：0 正常 / 1 禁用（禁号不可登录） */
    private Integer status;

    private LocalDateTime createdAt;
}
