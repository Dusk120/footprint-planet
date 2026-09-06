package com.qzxx.footprint_planet.vo;

import java.time.LocalDateTime;

/** 反馈记录（管理端列表） */
public record FeedbackVO(
        Long id,
        String content,
        String contact,
        Boolean handled,
        Long userId,
        String username,
        LocalDateTime createdAt
) {
}
