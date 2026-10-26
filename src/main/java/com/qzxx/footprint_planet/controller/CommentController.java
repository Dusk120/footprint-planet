package com.qzxx.footprint_planet.controller;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.CommentDTO;
import com.qzxx.footprint_planet.dto.request.CommentLikeDTO;
import com.qzxx.footprint_planet.service.CommentService;
import com.qzxx.footprint_planet.vo.CommentVO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes/{noteId}/comments")
public class CommentController {

    @Resource
    private CommentService commentService;

    @GetMapping
    public Result<PageVO<CommentVO>> getComments(@PathVariable Long noteId,
                                                 @RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.getComments(noteId, page, pageSize));
    }

    @PostMapping
    public Result<CommentVO> addComment(@PathVariable Long noteId,
                                        @RequestBody @Valid CommentDTO commentDTO) {
        return Result.success(commentService.addComment(noteId, commentDTO));
    }

    @DeleteMapping("/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long noteId,
                                      @PathVariable Long commentId) {
        commentService.deleteComment(noteId, commentId);
        return Result.success();
    }

    @PostMapping("/{commentId}/like")
    public Result<NoteLikeVO> likeComment(@PathVariable Long noteId,
                                          @PathVariable Long commentId,
                                          @RequestBody @Valid CommentLikeDTO commentLikeDTO) {
        return Result.success(commentService.likeComment(noteId, commentId, commentLikeDTO));
    }
}
