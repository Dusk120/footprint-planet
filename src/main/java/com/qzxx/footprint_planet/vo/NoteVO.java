package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 社区笔记视图（列表 / 详情 / 发布响应） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteVO {

    private Long id;

    private String title;

    private String content;

    private String image;

    private List<String> photos;

    private LocationVO location;

    private String author;

    private Long authorId;

    private String authorAvatar;

    private Integer likes;

    private Boolean liked;

    private LocalDateTime createdAt;
}
