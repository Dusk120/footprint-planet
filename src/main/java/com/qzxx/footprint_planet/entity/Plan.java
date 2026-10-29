package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("plan")
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户（owner） */
    private Long userId;

    private String name;

    private String destination;

    private String location;

    /** 封面图（可空） */
    private String image;

    private LocalDate startDate;

    private Boolean isPublic;

    /** 乐观锁版本号（写操作 +1） */
    @Version
    private Integer version;

    /** 协作锁定（true 锁定 / false 未锁定） */
    private Boolean collabLocked;

    /** 创作截止时间（可空 = 不限时） */
    private LocalDateTime collabDeadline;

    private LocalDateTime createdAt;
}
