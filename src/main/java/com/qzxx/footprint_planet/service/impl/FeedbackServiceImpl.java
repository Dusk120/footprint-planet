package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.qzxx.footprint_planet.dto.request.FeedbackDTO;
import com.qzxx.footprint_planet.entity.Feedback;
import com.qzxx.footprint_planet.mapper.FeedbackMapper;
import com.qzxx.footprint_planet.service.FeedbackService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackServiceImpl implements FeedbackService {
    @Resource
    private FeedbackMapper feedbackMapper;

    @Override
    public void submitFeedback(FeedbackDTO feedbackDTO) {
        Feedback feedback = new Feedback();
        BeanUtil.copyProperties(feedbackDTO, feedback);
        feedback.setHandled(false);
        feedback.setCreatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }
}
