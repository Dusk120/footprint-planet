package com.qzxx.footprint_planet.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeadlineDTO {

    /** 创作截止时间（可空 = 取消截止时间） */
    private LocalDateTime deadline;
}
