package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.CommentDTO;
import com.qzxx.footprint_planet.dto.request.CommentLikeDTO;
import com.qzxx.footprint_planet.vo.CommentVO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.PageVO;

/** 笔记评论（列表 / 发布回复 / 删除 / 点赞） */
public interface CommentService {

    PageVO<CommentVO> getComments(Long noteId, Integer page, Integer pageSize);

    CommentVO addComment(Long noteId, CommentDTO commentDTO);

    void deleteComment(Long noteId, Long commentId);

    NoteLikeVO likeComment(Long noteId, Long commentId, CommentLikeDTO commentLikeDTO);
}
