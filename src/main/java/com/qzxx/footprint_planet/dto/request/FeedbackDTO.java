package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FeedbackDTO(
        @NotBlank(message = "反馈内容不能为空")
        String content,

        String contact
) {
}
