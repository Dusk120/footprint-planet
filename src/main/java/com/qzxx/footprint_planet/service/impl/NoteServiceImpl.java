package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.LocationDTO;
import com.qzxx.footprint_planet.dto.request.NoteDTO;
import com.qzxx.footprint_planet.dto.request.NoteLikeDTO;
import com.qzxx.footprint_planet.dto.request.NoteQueryDTO;
import com.qzxx.footprint_planet.entity.Note;
import com.qzxx.footprint_planet.entity.NoteLike;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.mapper.NoteLikeMapper;
import com.qzxx.footprint_planet.mapper.NoteMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.NoteService;
import com.qzxx.footprint_planet.vo.LocationVO;
import com.qzxx.footprint_planet.vo.NoteLikeVO;
import com.qzxx.footprint_planet.vo.NoteVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl implements NoteService {

    @Resource
    private NoteMapper noteMapper;
    @Resource
    private NoteLikeMapper noteLikeMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private VisitedCityMapper visitedCityMapper;

    @Override
    public PageVO<NoteVO> getNotes(NoteQueryDTO noteQueryDTO) {
        Page<Note> page = new Page<>(noteQueryDTO.getPage(), noteQueryDTO.getPageSize());
        if ("hot".equals(noteQueryDTO.getSort())) {
            page.addOrder(OrderItem.desc("likes"), OrderItem.desc("created_at"));
        } else {
            page.addOrder(OrderItem.desc("created_at"), OrderItem.desc("likes"));
        }
        String kw = noteQueryDTO.getKeyword();
        Page<Note> resultPage = noteMapper.selectPage(page,
                new LambdaQueryWrapper<Note>()
                        .eq(noteQueryDTO.getAuthorId() != null, Note::getAuthorId, noteQueryDTO.getAuthorId())
                        .and(StringUtils.hasText(kw), w -> w
                                .like(Note::getTitle, kw).or()
                                .like(Note::getContent, kw).or()
                                .like(Note::getAuthorNickname, kw).or()
                                .like(Note::getLocationName, kw).or()
                                .like(Note::getLocationProvince, kw)));

        List<Note> records = resultPage.getRecords();
        Map<Long, User> authorMap = loadAuthors(records);
        Set<Long> likedNoteIds = loadLikedNoteIds(records);

        List<NoteVO> list = new ArrayList<>();
        for (Note note : records) {
            list.add(toVO(note, authorMap.get(note.getAuthorId()), likedNoteIds.contains(note.getId())));
        }

        PageVO<NoteVO> pageVO = new PageVO<>();
        pageVO.setList(list);
        pageVO.setTotal(resultPage.getTotal());
        pageVO.setPage(resultPage.getCurrent());
        pageVO.setPageSize(resultPage.getSize());
        return pageVO;
    }

    @Override
    public NoteVO addNote( NoteDTO noteDTO) {
        Note note = new Note();
        Long currentId = BaseContext.getCurrentId();
        BeanUtil.copyProperties(noteDTO, note);
        note.setAuthorId(currentId);
        User user = userMapper.selectById(currentId);
        note.setAuthorNickname(user.getNickname());
        LocationDTO location = noteDTO.getLocation();
        if (location != null) {
            Long count = visitedCityMapper.selectCount(new LambdaQueryWrapper<VisitedCity>()
                    .eq(VisitedCity::getUserId, currentId)
                    .eq(VisitedCity::getCityName, location.getName())
                    .eq(VisitedCity::getProvince, location.getProvince()));
            if (count == 0){
                throw new BusinessException(400, "该城市还未点亮");
            }
            note.setLocationName(location.getName());
            note.setLocationProvince(location.getProvince());
        }
        note.setLikes(0);
        note.setCreatedAt(java.time.LocalDateTime.now());
        noteMapper.insert(note);
        return toVO(note, user, false);
    }

    @Override
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new BusinessException(404, "笔记不存在");
        }
        if (!note.getAuthorId().equals(BaseContext.getCurrentId())) {
            throw new BusinessException(403, "无权限删除");
        }
        noteMapper.deleteById(id);
    }

    @Override
    @Transactional
    public NoteLikeVO likeNote(Long id, NoteLikeDTO noteLikeDTO) {
        Long userId = BaseContext.getCurrentId();
        Note note = noteMapper.selectById(id);
        if(note == null){
            throw new BusinessException(404, "笔记不存在");
        }
        int likeCount = noteLikeMapper.selectCount(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getUserId, userId)
                .eq(NoteLike::getNoteId, id)).intValue();
        NoteLikeVO noteLikeVO = new NoteLikeVO();
        noteLikeVO.setLiked(noteLikeDTO.getLike());
        if (noteLikeDTO.getLike() && likeCount == 0) {
            noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                    .eq(Note::getId, id)
                    .setSql("likes = likes + 1"));
            NoteLike noteLike = new NoteLike();
            noteLike.setUserId(userId);
            noteLike.setNoteId(id);
            noteLikeMapper.insert(noteLike);
        }
        else if (!noteLikeDTO.getLike() && likeCount > 0){
            noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                    .eq(Note::getId, id)
                    .setSql("likes = likes - 1"));
            noteLikeMapper.delete(new LambdaQueryWrapper<NoteLike>()
                    .eq(NoteLike::getUserId, userId)
                    .eq(NoteLike::getNoteId, id));
        }
        noteLikeVO.setLikes(noteMapper.selectById(id).getLikes());
        return noteLikeVO;
    }

    /** 当前登录用户在这批笔记中已点赞的 noteId 集合；未登录返回空集（liked 全为 false） */
    private Set<Long> loadLikedNoteIds(List<Note> notes) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null || notes.isEmpty()) {
            return Set.of();
        }
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        return noteLikeMapper.selectList(new LambdaQueryWrapper<NoteLike>()
                        .select(NoteLike::getNoteId)
                        .in(NoteLike::getNoteId, noteIds)
                        .eq(NoteLike::getUserId, userId))
                .stream().map(NoteLike::getNoteId).collect(Collectors.toSet());
    }

    /** 一次查出当页笔记的作者，避免逐条查询 */
    private Map<Long, User> loadAuthors(List<Note> notes) {
        Set<Long> authorIds = notes.stream()
                .map(Note::getAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> authorMap = new HashMap<>();
        for (User user : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, authorIds))) {
            authorMap.put(user.getId(), user);
        }
        return authorMap;
    }

    private NoteVO toVO(Note note, User author, boolean liked) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setImage(note.getImage());
        vo.setPhotos(note.getPhotos());
        vo.setAuthor(note.getAuthorNickname());
        vo.setAuthorId(note.getAuthorId());
        vo.setAuthorAvatar(author != null ? author.getAvatar() : User.DEFAULT_AVATAR);
        vo.setLikes(note.getLikes());
        vo.setLiked(liked);
        vo.setCreatedAt(note.getCreatedAt());
        if (note.getLocationName() != null || note.getLocationProvince() != null) {
            vo.setLocation(new LocationVO(note.getLocationName(), note.getLocationProvince()));
        }
        return vo;
    }
}
