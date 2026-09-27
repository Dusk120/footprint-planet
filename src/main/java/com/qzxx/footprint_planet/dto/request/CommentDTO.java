package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发布评论 / 回复的请求体 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过 500 字")
    private String content;

    /**
     * 回复的目标评论ID。为空表示发顶层评论；
     * 非空时必须是同一篇笔记下的顶层评论（只允许一级回复）。
     */
    private Long parentId;
}
