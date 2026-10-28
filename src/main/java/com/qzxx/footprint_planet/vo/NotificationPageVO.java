package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通知列表游标分页响应。
 *
 * 与翻页器（page/pageSize/total）不同，滚动加载只给「这批数据 + 下一页游标 + 是否还有」，
 * 分页决策（hasMore）由后端判定，前端不做任何分页计算。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPageVO {

    /** 当前这一批通知（按 id 倒序） */
    private List<NotificationVO> list;

    /** 下一页游标：本批最后一条的 id，没有更多时为 null */
    private Long nextCursor;

    /** 是否还有下一页 */
    private boolean hasMore;

    /** 当前 type 的未读数（tab 角标用） */
    private long unreadCount;

    /** 全部类型的未读总数（铃铛红点用） */
    private long totalUnreadCount;
}
