package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    public static final String DEFAULT_AVATAR = "https://hcy120.oss-cn-hangzhou.aliyuncs.com/20200414210134_qbeyi.jpg";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String motto;

    private Integer totalKm;

    /** 角色：ADMIN / USER（默认 USER） */
    private String role;

    /** 状态：0 正常 / 1 禁用（禁号不可登录） */
    private Integer status;

    private LocalDateTime createdAt;

    /** 未设置头像时回落到默认图（覆写 Lombok 生成的 getter，register/login/me/作者主页全走这里） */
    public String getAvatar() {
        return avatar == null || avatar.isBlank() ? DEFAULT_AVATAR : avatar;
    }
}
