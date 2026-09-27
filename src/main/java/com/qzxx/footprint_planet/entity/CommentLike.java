package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 评论点赞记录（评论与回复共用本表，回复也是 note_comment 的行） */
@Data
@TableName("comment_like")
public class CommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被点赞的评论ID（含回复） */
    private Long commentId;

    private Long userId;

    private LocalDateTime createdAt;
}
