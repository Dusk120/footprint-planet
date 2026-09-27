package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 评论点赞 / 取消点赞的请求体（与笔记点赞同构） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentLikeDTO {

    @NotNull(message = "like 不能为空")
    private Boolean like;
}
