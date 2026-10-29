package com.qzxx.footprint_planet.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CollaboratorVO {

    private Long userId;

    private String nickname;

    private String avatar;

    /** owner / editor / viewer */
    private String role;

    private LocalDateTime joinedAt;
}
