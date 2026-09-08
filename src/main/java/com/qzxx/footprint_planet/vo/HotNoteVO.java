package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 点赞榜 Top5 笔记 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotNoteVO {

    private Long id;

    private String title;

    private String author;

    private Integer likes;

    private LocalDateTime createdAt;
}
