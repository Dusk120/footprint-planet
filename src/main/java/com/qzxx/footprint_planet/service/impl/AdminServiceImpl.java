package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.RedisKeys;
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
import java.time.temporal.ChronoUnit;
import java.util.*;
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
        LocalDate start = now.minusDays(6);

        // 一条 SQL 覆盖整周并按天聚合：WHERE 走 idx_created 索引区间，GROUP BY 只带回有数据的日期
        List<Map<String, Object>> noteTrends = noteMapper.selectMaps(new QueryWrapper<Note>().
                select("DATE_FORMAT(created_at, '%Y-%m-%d') AS d", "COUNT(*) AS c").
                ge("created_at", start.atStartOfDay()).
                groupBy("d"));

        Map<String, Long> countByDate = new HashMap<>();
        for (Map<String, Object> noteTrend : noteTrends) {
            countByDate.put((String) noteTrend.get("d"), ((Number) noteTrend.get("c")).longValue());
        }
        // 关键：GROUP BY 不会返回「零笔记」的日期，这里按 7 天补全，缺的记 0
        List<NoteTrendVO> noteTrendVOList = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(now); date = date.plusDays(1)) {
            noteTrendVOList.add(new NoteTrendVO(date.toString(), countByDate.getOrDefault(date.toString(), 0L)));
        }
        adminDashboardVO.setTrend(noteTrendVOList);
        adminDashboardVO.setHotCities(hotService.getHotDestinations());
        // 榜单只需要标题/作者/点赞数/时间：content 是 TEXT、image 可能存 base64、photos 是 JSON，
        // 这四列对 Top5 展示毫无用处，限定列可避免每条笔记搬运几十 KB
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .select(Note::getId, Note::getTitle, Note::getAuthorNickname, Note::getLikes, Note::getCreatedAt)
                .orderByDesc(Note::getLikes)
                .last("limit 5"));
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
        // 只查两列并交给数据库 GROUP BY，避免把整页用户的全部笔记（content TEXT / image base64 / photos JSON）拉进内存
        // COUNT(*) 在 MySQL 是 BIGINT，驱动返回 Long，必须走 Number 转换，直接 (Integer) 会 ClassCastException
        Map<Long, Integer> noteMap = noteMapper.selectMaps(new QueryWrapper<Note>().
                select("author_id AS authorId","COUNT(*) AS cnt").
                in("author_id", userIdList).
                groupBy("authorId")).stream().
                collect(Collectors.toMap(
                        m -> ((Number) m.get("authorId")).longValue(),
                        m -> ((Number) m.get("cnt")).intValue()
                ));

        // 统计每个用户访问城市数量: userId -> count
        // 与上面的 noteMap 同款处理：只查两列交给数据库聚合，
        // visited_city 的 records 是 JSON 列，整行拉回来只为数个数纯属浪费
        Map<Long, Integer> visitedCityMap = visitedCityMapper.selectMaps(new QueryWrapper<VisitedCity>().
                select("user_id AS userId", "COUNT(*) AS cnt").
                in("user_id", userIdList).
                groupBy("userId")).stream().
                collect(Collectors.toMap(
                        m -> ((Number) m.get("userId")).longValue(),
                        m -> ((Number) m.get("cnt")).intValue()
                ));
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
            // 旅行天数不再落库，实时计算
            adminUserDetailVO.setTotalDays(user.getCreatedAt() == null ? 0
                    : (int) ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now()));
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
