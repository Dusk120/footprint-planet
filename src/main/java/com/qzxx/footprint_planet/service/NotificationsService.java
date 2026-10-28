package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.vo.NotificationPageVO;

public interface NotificationsService {

    /**
     * 游标分页拉取某类通知（点赞 / 评论）。
     * @param type     通知分类，取值仅两种：{@code "like"} 或 {@code "comment"}。
     * @param cursor   游标（上一页最后一条通知的 id），第一页传 {@code null}
     * @param pageSize 每页条数（实现需多查 1 条判断 hasMore）。
     */
    NotificationPageVO getNotifications(String type, Long cursor, Integer pageSize);

    Void readNotification(Long id);

    Void readAllNotifications(String type);

    /**
     * 触发通知：sender 固定为当前登录用户，落库 + 实时推送（不通知自己）。
     *
     * @param receiverId 接收者用户 id
     * @param type       类型：like_note / comment / reply / like_comment
     * @param noteId     关联笔记 id（可空）
     * @param noteTitle  关联笔记标题（可空）
     * @param content    摘要文案，如「点赞了你的笔记」
     */
    void notify(Long receiverId, String type, Long noteId, String noteTitle, String content);
}
