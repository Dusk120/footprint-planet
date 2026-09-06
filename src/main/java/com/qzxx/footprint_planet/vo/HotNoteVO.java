package com.qzxx.footprint_planet.vo;

import java.time.LocalDateTime;

/** 点赞榜 Top5 笔记 */
public record HotNoteVO(
        Long id,
        String title,
        String author,
        Integer likes,
        LocalDateTime createdAt
) {
}
