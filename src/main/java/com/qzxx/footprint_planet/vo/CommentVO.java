package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** 笔记评论视图（顶层评论 / 一级回复共用） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;

    private Long noteId;

    /** 评论者用户ID（与 NoteVO 的 authorId 命名保持一致） */
    private Long authorId;

    /** 评论者昵称 */
    private String author;

    private String authorAvatar;

    /** 父评论ID；null = 顶层评论，非 null = 回复 */
    private Long parentId;

    private String content;

    private Integer likes;

    /** 当前登录用户是否点过赞（未登录恒为 false） */
    private Boolean liked;

    /**
     * 当前登录用户能否删除这条评论：管理员 / 笔记作者 / 评论作者三者之一。
     * 由后端算好，前端直接用它决定要不要显示删除按钮。
     */
    private Boolean canDelete;

    /** 该评论下的一级回复（按时间正序）；只对顶层评论填充，回复本身为 null */
    private List<CommentVO> replies;

    private LocalDateTime createdAt;
}
