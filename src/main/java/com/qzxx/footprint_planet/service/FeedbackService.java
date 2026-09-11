package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.FeedbackDTO;


public interface FeedbackService {

    void submitFeedback(FeedbackDTO feedbackDTO);
}
