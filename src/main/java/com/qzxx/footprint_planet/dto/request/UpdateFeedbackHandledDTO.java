package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 管理端 5.4.2：标记反馈处理状态 */
@Data
public class UpdateFeedbackHandledDTO {
    @NotNull(message = "handled 不能为空")
    private Boolean handled;
}
