package com.qzxx.footprint_planet.controller;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.FeedbackDTO;
import com.qzxx.footprint_planet.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {
    @Resource
    private FeedbackService feedbackService;

    @PostMapping
    public Result<Void> submitFeedback(@RequestBody @Valid FeedbackDTO feedbackDTO) {
        feedbackService.submitFeedback(feedbackDTO);
        return Result.success();
    }
}
