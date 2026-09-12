package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzxx.footprint_planet.entity.Feedback;
import com.qzxx.footprint_planet.entity.Note;
import com.qzxx.footprint_planet.mapper.FeedbackMapper;
import com.qzxx.footprint_planet.mapper.NoteMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.service.HotService;
import com.qzxx.footprint_planet.vo.AdminDashboardVO;
import com.qzxx.footprint_planet.vo.HotNoteVO;
import com.qzxx.footprint_planet.vo.NoteTrendVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public AdminDashboardVO getAdminDashboard() {
        AdminDashboardVO adminDashboardVO = new AdminDashboardVO();
        adminDashboardVO.setUserCount(userMapper.selectCount(null));
        adminDashboardVO.setNoteCount(noteMapper.selectCount(null));
        adminDashboardVO.setFeedbackCount(feedbackMapper.selectCount(null));
        adminDashboardVO.setPendingFeedbackCount(feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>().eq(Feedback::getHandled, 0)));
        LocalDate now = LocalDate.now();
        List<NoteTrendVO> noteTrendVOList = new ArrayList<>();
        for(int i=6; i>=0; i--){
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
}
