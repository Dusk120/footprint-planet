package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收者用户 id */
    private Long userId;

    /** 类型：like_note / comment / reply / like_comment */
    private String type;

    /** 触发者用户 id */
    private Long senderId;

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
