package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDTO {

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private String contact;
}
