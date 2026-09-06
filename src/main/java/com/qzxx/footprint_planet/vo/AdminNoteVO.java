package com.qzxx.footprint_planet.vo;

import java.time.LocalDateTime;

/** 管理端笔记行（列表 / 详情共用；authorId 便于跳转作者） */
public record AdminNoteVO(
        Long id,
        String title,
        String content,
        String image,
        String author,
        Long authorId,
        Integer likes,
        LocationVO location,
        LocalDateTime createdAt
) {
}
