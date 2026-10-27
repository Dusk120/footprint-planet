package com.qzxx.footprint_planet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.CommentDTO;
import com.qzxx.footprint_planet.dto.request.CommentLikeDTO;
import com.qzxx.footprint_planet.entity.CommentLike;
import com.qzxx.footprint_planet.entity.Note;
import com.qzxx.footprint_planet.entity.NoteComment;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.CommentLikeMapper;
import com.qzxx.footprint_planet.mapper.NoteCommentMapper;
import com.qzxx.footprint_planet.mapper.NoteMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.service.CommentService;
import com.qzxx.footprint_planet.service.NotificationsService;
import com.qzxx.footprint_planet.vo.CommentVO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CommentServiceImpl implements CommentService {

    @Resource
    private NoteCommentMapper noteCommentMapper;
    @Resource
    private CommentLikeMapper commentLikeMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private NotificationsService notificationsService;


    @Override
    public PageVO<CommentVO> getComments(Long noteId, Integer page, Integer pageSize) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404,"笔记不存在");
        }
        Page<NoteComment> noteCommentPage = new Page<>(page, pageSize);
        Page<NoteComment> result = noteCommentMapper.selectPage(noteCommentPage, new LambdaQueryWrapper<NoteComment>()
                .eq(NoteComment::getNoteId, noteId).isNull(NoteComment::getParentId)
                .orderByDesc(NoteComment::getCreatedAt, NoteComment::getLikes));
        List<NoteComment> tops = result.getRecords();
        if (tops.isEmpty()) {
            return new PageVO<>(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        // 顶层评论（父评论）的 id：拿它们去把这一页所有回复一次性查出来
        List<Long> commentIds = tops.stream()
                .map(NoteComment::getId).filter(Objects::nonNull)
                .distinct()
                .toList();
        // 回复：按评论 id 分组。
        List<NoteComment> replies = noteCommentMapper.selectList(new LambdaQueryWrapper<NoteComment>()
                .in(NoteComment::getParentId, commentIds)
                .orderByAsc(NoteComment::getCreatedAt));
        Map<Long, List<NoteComment>> replyMap = replies.stream()
                .collect(Collectors.groupingBy(NoteComment::getParentId));
        List<Long> userIds = Stream.concat(tops.stream(), replies.stream())
                .map(NoteComment::getUserId).distinct().toList();
        Map<Long, User> userIdToUser = userMapper.selectList(new LambdaQueryWrapper<User>()
                .in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        Long me = BaseContext.getCurrentId();
        // 这批评论（顶层 + 回复）的全部 id，用来把点赞查询限定在当页，而不是去查全站点赞
        List<Long> allCommentIds = Stream.concat(tops.stream(), replies.stream())
                .map(NoteComment::getId).toList();
        // 当前登录用户点过赞的 commentId；未登录直接给空集（liked 恒为 false），也省一次查询。
        // 注意这里只能赋值一次：下面要在 lambda 里引用它，被重新赋值就不是 effectively final 了
        Set<Long> likedIds = me == null ? Set.of()
                : commentLikeMapper.selectList(new LambdaQueryWrapper<CommentLike>()
                        .select(CommentLike::getCommentId)
                        .in(CommentLike::getCommentId, allCommentIds)
                        .eq(CommentLike::getUserId, me)).stream()
                .map(CommentLike::getCommentId).collect(Collectors.toSet());

        // liked 与 canDelete 都在后端按当前登录用户算好，前端只负责按结果渲染，不重复实现权限逻辑
        Long noteAuthorId = note.getAuthorId();
        boolean isAdmin = "ADMIN".equals(BaseContext.getCurrentRole());

        List<CommentVO> list = new ArrayList<>();
        for (NoteComment top : tops) {
            CommentVO vo = toCommentVO(top, userIdToUser, likedIds, me, isAdmin, noteAuthorId);
            // 顶层评论挂上它自己的回复；回复已在上面按 created_at 正序查好，这里不用再排序
            vo.setReplies(replyMap.getOrDefault(top.getId(), List.of()).stream()
                    .map(reply -> toCommentVO(reply, userIdToUser, likedIds, me, isAdmin, noteAuthorId))
                    .toList());
            list.add(vo);
        }
        // total 是「顶层评论」总数（分页只作用于顶层，不含回复），前端滚动分页靠它判断还有没有下一页
        return new PageVO<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 单条评论 → CommentVO。顶层评论与一级回复共用，字段完全一样，
     * 唯一的差别是 replies：这里统一给空数组，顶层的 replies 由调用方组装时覆盖掉。
     *
     * @param authorMap    评论者用户表（顶层 + 回复一次性查好），key = userId
     * @param likedIds     当前登录用户在这批评论里点过赞的 commentId 集合；未登录时为空集
     * @param me           当前登录用户 id；未登录为 null
     * @param isAdmin      当前登录用户是否为管理员
     * @param noteAuthorId 笔记作者 id（笔记作者可以删自己笔记下的任何评论与回复）
     */
    private CommentVO toCommentVO(NoteComment comment, Map<Long, User> authorMap, Set<Long> likedIds,
                                  Long me, boolean isAdmin, Long noteAuthorId) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setNoteId(comment.getNoteId());
        vo.setParentId(comment.getParentId());
        vo.setAuthorId(comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setLikes(comment.getLikes());        // 直接用冗余计数列，不用去 count 点赞表
        vo.setCreatedAt(comment.getCreatedAt());

        // 作者信息：外键 fk_comment_user 是级联删除，正常都能查到，仍做一层空值兜底
        User author = authorMap.get(comment.getUserId());
        vo.setAuthor(author != null ? author.getNickname() : "未知用户");
        vo.setAuthorAvatar(author != null ? author.getAvatar() : User.DEFAULT_AVATAR);

        // 未登录时 likedIds 是空集，这里自然为 false
        vo.setLiked(likedIds.contains(comment.getId()));
        // 删除权限：管理员 / 笔记作者 / 评论作者本人；me 为 null（未登录）时恒为 false
        vo.setCanDelete(isAdmin
                || (me != null && (me.equals(noteAuthorId) || me.equals(comment.getUserId()))));
        vo.setReplies(List.of());
        return vo;
    }

    @Override
    public CommentVO addComment(Long noteId, CommentDTO commentDTO) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404,"笔记不存在");
        }
        Long me = BaseContext.getCurrentId();
        Long parentId = commentDTO.getParentId();
        if (parentId != null) {
            NoteComment parent = noteCommentMapper.selectById(parentId);
            // 不存在、或不属于这条笔记，对外一律当作「父评论不存在」
            if (parent == null || !parent.getNoteId().equals(noteId)) {
                throw new BusinessException(404, "父评论不存在");
            }
            // 只支持一级回复：父评论自己必须也是顶层
            if (parent.getParentId() != null) {
                throw new BusinessException(400, "只支持一级回复");
            }
        }
        NoteComment newNoteComment = new NoteComment();
        newNoteComment.setNoteId(noteId);
        newNoteComment.setParentId(parentId);
        newNoteComment.setUserId(me);
        newNoteComment.setContent(commentDTO.getContent());
        newNoteComment.setLikes(0);
        newNoteComment.setCreatedAt(LocalDateTime.now());
        noteCommentMapper.insert(newNoteComment);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, me));
        Map<Long, User> authorMap = new HashMap<>();
        if(user != null) {
            authorMap.put(user.getId(), user);
        }
        boolean isAdmin = "ADMIN".equals(BaseContext.getCurrentRole());
        Long noteAuthorId = note.getAuthorId();
        // 顶层评论通知笔记作者；回复通知被回复的评论作者（notify 内部已排除自己）
        if (parentId == null) {
            notificationsService.notify(noteAuthorId, "comment", noteId, note.getTitle(), "评论了你的笔记");
        } else {
            NoteComment parent = noteCommentMapper.selectById(parentId);
            if (parent != null) {
                notificationsService.notify(parent.getUserId(), "reply", noteId, note.getTitle(), "回复了你的评论");
            }
        }
        return toCommentVO(newNoteComment, authorMap, Set.of(), me, isAdmin, noteAuthorId);
    }

    @Override
    public void deleteComment(Long noteId, Long commentId) {
        Long userId = BaseContext.getCurrentId();
        NoteComment comment = noteCommentMapper.selectById(commentId);
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        if (!comment.getNoteId().equals(noteId)) {
            throw new BusinessException(404, "评论不属于该笔记");
        }
        if (!comment.getUserId().equals(userId) && !"ADMIN".equals(BaseContext.getCurrentRole()) && !note.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "无权限删除");
        }
        noteCommentMapper.deleteById(commentId);
    }

    @Override
    @Transactional
    public NoteLikeVO likeComment(Long noteId, Long commentId, CommentLikeDTO commentLikeDTO) {
        Long userId = BaseContext.getCurrentId();
        NoteComment comment = noteCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }
        if (!comment.getNoteId().equals(noteId)) {
            throw new BusinessException(404, "评论不属于该笔记");
        }
        NoteLikeVO noteLikeVO = new NoteLikeVO();
        noteLikeVO.setLiked(commentLikeDTO.getLike());
        Long count = commentLikeMapper.selectCount(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, commentId)
                .eq(CommentLike::getUserId, userId));
        if (commentLikeDTO.getLike() && count==0){
            noteCommentMapper.update(null, new LambdaUpdateWrapper<NoteComment>()
                    .eq(NoteComment::getId, commentId)
                    .setSql("likes = likes + 1"));
            CommentLike commentLike=new CommentLike();
            commentLike.setUserId(userId);
            commentLike.setCommentId(commentId);
            commentLike.setCreatedAt(LocalDateTime.now());
            commentLikeMapper.insert(commentLike);
        } else if (!commentLikeDTO.getLike() && count==1){
            noteCommentMapper.update(null, new LambdaUpdateWrapper<NoteComment>()
                    .eq(NoteComment::getId, commentId)
                    .setSql("likes = likes - 1"));
            commentLikeMapper.delete(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, commentId)
                    .eq(CommentLike::getUserId, userId));
        }
        noteLikeVO.setLikes(noteCommentMapper.selectById(commentId).getLikes());
        // 点赞成功才通知（取消点赞不通知）；notify 内部已排除自己给自己点赞
        if (commentLikeDTO.getLike() && count == 0) {
            Note note = noteMapper.selectById(noteId);
            notificationsService.notify(comment.getUserId(), "like_comment", noteId,
                    note != null ? note.getTitle() : null, "点赞了你的评论");
        }
        return noteLikeVO;
    }
}
