package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 反馈记录（管理端列表） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackVO {

    private Long id;

    private String content;

    private String contact;

    private List<String> images;

    private Boolean handled;

    private LocalDateTime createdAt;
}
