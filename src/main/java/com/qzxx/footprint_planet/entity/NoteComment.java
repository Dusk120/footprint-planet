package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记评论。
 * <p>只支持一级回复：{@code parentId} 为 null 是顶层评论，非 null 表示对某条顶层评论的回复。
 * 回复的 parentId 一定指向顶层评论，不存在「回复的回复」。</p>
 */
@Data
@TableName("note_comment")
public class NoteComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    /** 评论者用户ID */
    private Long userId;

    /** 父评论ID；null = 顶层评论，非 null = 一级回复 */
    private Long parentId;

    private String content;

    /** 点赞数（冗余计数，与 comment_like 的行数对应） */
    private Integer likes;

    private LocalDateTime createdAt;
}
