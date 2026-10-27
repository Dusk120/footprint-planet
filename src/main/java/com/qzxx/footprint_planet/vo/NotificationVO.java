package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 消息通知视图（列表元素 / WebSocket 推送 payload，两者字段一致） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;

    /** 类型：like_note 点赞笔记 / comment 评论笔记 / reply 回复评论 / like_comment 点赞评论 */
    private String type;

    /** 触发者昵称 */
    private String senderNickname;

    private String senderAvatar;

    private Long noteId;

    private String noteTitle;

    /** 摘要文案，如「点赞了你的笔记」 */
    private String content;

    /** 已读：0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime createdAt;
}
