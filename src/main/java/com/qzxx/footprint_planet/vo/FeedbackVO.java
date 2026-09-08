package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 反馈记录（管理端列表） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackVO {

    private Long id;

    private String content;

    private String contact;

    private Boolean handled;

    private Long userId;

    private String username;

    private LocalDateTime createdAt;
}
