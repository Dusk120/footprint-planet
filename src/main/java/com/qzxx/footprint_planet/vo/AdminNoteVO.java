package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 管理端笔记行（列表 / 详情共用；authorId 便于跳转作者） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNoteVO {

    private Long id;

    private String title;

    private String content;

    private String image;

    private String author;

    private Long authorId;

    private Integer likes;

    private LocationVO location;

    private LocalDateTime createdAt;
}
