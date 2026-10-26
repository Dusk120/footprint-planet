package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.RedisKeys;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.ResetPasswordDTO;
import com.qzxx.footprint_planet.dto.request.UpdateUserStatusDTO;
import com.qzxx.footprint_planet.entity.Feedback;
import com.qzxx.footprint_planet.entity.Note;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.mapper.FeedbackMapper;
import com.qzxx.footprint_planet.mapper.NoteMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.service.HotService;
import com.qzxx.footprint_planet.vo.*;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private FeedbackMapper feedbackMapper;
    @Resource
    private HotService hotService;
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO adminDashboardVO = new AdminDashboardVO();
        adminDashboardVO.setUserCount(userMapper.selectCount(null));
        adminDashboardVO.setNoteCount(noteMapper.selectCount(null));
        adminDashboardVO.setFeedbackCount(feedbackMapper.selectCount(null));
        adminDashboardVO.setPendingFeedbackCount(feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>().eq(Feedback::getHandled, 0)));
        LocalDate now = LocalDate.now();
        List<NoteTrendVO> noteTrendVOList = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            NoteTrendVO noteTrendVO = new NoteTrendVO();
            LocalDate date = now.minusDays(i);
            Long l = noteMapper.selectCount(new LambdaQueryWrapper<Note>().like(Note::getCreatedAt, date));
            noteTrendVO.setDate(date.toString());
            noteTrendVO.setNoteCount(l);
            noteTrendVOList.add(noteTrendVO);
        }
        adminDashboardVO.setTrend(noteTrendVOList);
        adminDashboardVO.setHotCities(hotService.getHotDestinations());
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>().orderByDesc(Note::getLikes).last("limit 5"));
        List<HotNoteVO> hotNoteVOList = notes.stream().map(note -> {
            HotNoteVO hotNoteVO = new HotNoteVO();
            BeanUtil.copyProperties(note, hotNoteVO);
            hotNoteVO.setAuthor(note.getAuthorNickname());
            return hotNoteVO;
        }).toList();
        adminDashboardVO.setHotNotes(hotNoteVOList);
        return adminDashboardVO;
    }

    @Override
    public PageVO<AdminUserVO> getUsers(String keyword, int page, int pageSize) {
        Page<User> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>();
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(User::getUsername, keyword).or().like(User::getNickname, keyword);
        }
        Page<User> result = userMapper.selectPage(p, queryWrapper);
        List<User> records = result.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageVO<>(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<Long> userIdList = records.stream().map(User::getId).toList();

        // 统计每个用户笔记数量: authorId -> count
        Map<Long, Integer> noteMap = noteMapper.selectList(new LambdaQueryWrapper<Note>().
                        in(Note::getAuthorId, userIdList)).stream().
                collect(Collectors.groupingBy(Note::getAuthorId, Collectors.summingInt(e -> 1)));

        // 统计每个用户访问城市数量: userId -> count
        Map<Long, Integer> visitedCityMap = visitedCityMapper.selectList(new LambdaQueryWrapper<VisitedCity>().
                        in(VisitedCity::getUserId, userIdList)).stream().
                collect(Collectors.groupingBy(VisitedCity::getUserId, Collectors.summingInt(e -> 1)));
        List<AdminUserVO> list = records.stream().map(user -> {
            AdminUserVO adminUserVO = new AdminUserVO();
            BeanUtil.copyProperties(user, adminUserVO);
            adminUserVO.setNoteCount(noteMap.getOrDefault(user.getId(), 0));
            adminUserVO.setVisitedCount(visitedCityMap.getOrDefault(user.getId(), 0));
            return adminUserVO;
        }).toList();
        return new PageVO<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminUserDetailVO getUser(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            AdminUserDetailVO adminUserDetailVO = new AdminUserDetailVO();
            BeanUtil.copyProperties(user, adminUserDetailVO);
            adminUserDetailVO.setNoteCount(noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getAuthorId, user.getId())).intValue());
            adminUserDetailVO.setVisitedCount(visitedCityMapper.selectCount(new LambdaQueryWrapper<VisitedCity>().eq(VisitedCity::getUserId, user.getId())).intValue());
            adminUserDetailVO.setVisitedProvinces(visitedCityMapper.countProvincesByUserId(id));
            return adminUserDetailVO;
        }
        throw new BusinessException(404, "用户不存在");
    }

    @Override
    public Void updateStatus(Long id, UpdateUserStatusDTO updateUserStatusDTO) {
        User user = userMapper.selectById(id);
        if(user == null)
            throw new BusinessException(404, "用户不存在");
        if(id.equals(BaseContext.getCurrentId()))
            throw new BusinessException(400, "不能修改自己的状态");
        user.setStatus(updateUserStatusDTO.getStatus());
        userMapper.updateById(user);
        try {
            redisTemplate.delete(RedisKeys.userStatus(id));
        } catch (Exception e) {
            // 缓存失效失败不阻断主流程；Redis 挂时拦截器会降级回库，禁用仍即时生效
        }
        return null;
    }

    @Override
    public Void updatePassword(Long id, ResetPasswordDTO resetPasswordDTO) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(resetPasswordDTO.getPassword()));
            userMapper.updateById(user);
            return null;
        }
        throw new BusinessException(404, "用户不存在");
    }

    @Override
    public PageVO<AdminNoteVO> getAdminNotes(String keyword, int page, int pageSize) {
        Page<Note> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Note> queryWrapper = new LambdaQueryWrapper<Note>();
        queryWrapper.orderByDesc(Note::getCreatedAt);
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like(Note::getTitle, keyword).or()
                    .like(Note::getContent, keyword).or()
                    .like(Note::getAuthorNickname, keyword);
        }
        Page<Note> result = noteMapper.selectPage(p, queryWrapper);
        List<Note> records = result.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageVO<>(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<AdminNoteVO> list = records.stream().map(note -> {
            AdminNoteVO adminNoteVO = new AdminNoteVO();
            BeanUtil.copyProperties(note, adminNoteVO);
            adminNoteVO.setAuthor(note.getAuthorNickname());
            adminNoteVO.setLocation(new LocationVO(note.getLocationName(), note.getLocationProvince()));
            return adminNoteVO;
        }).toList();
        return new PageVO<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            noteMapper.deleteById(id);
        } else {
            throw new BusinessException(404, "笔记不存在");
        }
        return null;
    }

    @Override
    public PageVO<FeedbackVO> getFeedbacks(Integer handled, int page, int pageSize) {
        Page<Feedback> p = new Page<>(page, pageSize);
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<Feedback>();
        queryWrapper.orderByDesc(Feedback::getCreatedAt);
        if (handled != null && (handled == 1 || handled == 0)) {
            queryWrapper.eq(Feedback::getHandled, handled);
        }
        Page<Feedback> result = feedbackMapper.selectPage(p, queryWrapper);
        List<Feedback> records = result.getRecords();
        if (CollectionUtil.isEmpty(records)) {
            return new PageVO<>(Collections.emptyList(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        List<FeedbackVO> list = records.stream().map(feedback -> {
            FeedbackVO feedbackVO = new FeedbackVO();
            BeanUtil.copyProperties(feedback, feedbackVO);
            return feedbackVO;
        }).toList();
        return new PageVO<>(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public Void updateHandled(Long id, boolean handled) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedback.setHandled(handled);
            feedbackMapper.updateById(feedback);
        } else {
            throw new BusinessException(404, "反馈不存在");
        }
        return null;
    }

    @Override
    public Void deleteFeedback(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedbackMapper.deleteById(id);
        } else {
            throw new BusinessException(404, "反馈不存在");
        }
        return null;
    }
}
