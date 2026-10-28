package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论列表游标分页响应。
 *
 * 分页只作用于「顶层评论」（parentId 为 null），回复跟随各自顶层评论一次性返回，
 * 不单独分页。滚动加载只给「这批数据 + 下一页游标 + 是否还有」，分页决策（hasMore）由后端判定。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentPageVO {

    private List<CommentVO> list;

    /** 下一页游标：本批最后一条顶层评论的 id，没有更多时为 null */
    private Long nextCursor;

    /** 是否还有下一页 */
    private boolean hasMore;
}
