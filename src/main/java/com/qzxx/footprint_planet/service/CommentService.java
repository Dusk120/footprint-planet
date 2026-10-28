package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.CommentDTO;
import com.qzxx.footprint_planet.dto.request.CommentLikeDTO;
import com.qzxx.footprint_planet.vo.CommentPageVO;
import com.qzxx.footprint_planet.vo.CommentVO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;

/** 笔记评论（列表 / 发布回复 / 删除 / 点赞） */
public interface CommentService {

    /**
     * 游标分页拉取某条笔记的顶层评论（回复跟随顶层评论返回，不单独分页）。
     *
     * @param noteId   笔记 id
     * @param cursor   游标（上一页最后一条顶层评论的 id），第一页传 {@code null}
     * @param pageSize 每页条数（实现需多查 1 条判断 hasMore）
     */
    CommentPageVO getComments(Long noteId, Long cursor, Integer pageSize);

    CommentVO addComment(Long noteId, CommentDTO commentDTO);

    void deleteComment(Long noteId, Long commentId);

    NoteLikeVO likeComment(Long noteId, Long commentId, CommentLikeDTO commentLikeDTO);
}
