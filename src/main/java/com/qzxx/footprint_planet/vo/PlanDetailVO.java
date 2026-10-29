package com.qzxx.footprint_planet.vo;

import com.qzxx.footprint_planet.entity.PlanItem;
import com.qzxx.footprint_planet.entity.PlanLeg;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PlanDetailVO {

    private Long id;

    private Long userId;

    private String name;

    private String destination;

    private String location;

    private String image;

    private LocalDate startDate;

    private Boolean isPublic;

    private Integer version;

    private Boolean collabLocked;

    private LocalDateTime collabDeadline;

    private LocalDateTime createdAt;

    /** 当前登录用户在此计划中的角色 */
    private String myRole;

    private List<PlanItem> items;

    private List<PlanLeg> legs;

    private List<CollaboratorVO> collaborators;
}
