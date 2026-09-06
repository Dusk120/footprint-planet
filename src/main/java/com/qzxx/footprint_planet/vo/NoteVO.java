package com.qzxx.footprint_planet.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 社区笔记视图（列表 / 详情 / 发布响应） */
public record NoteVO(
        Long id,
        String title,
        String content,
        String image,
        List<String> photos,
        LocationVO location,
        String author,
        Long authorId,
        Integer likes,
        List<String> likedBy,
        LocalDateTime createdAt
) {
}
