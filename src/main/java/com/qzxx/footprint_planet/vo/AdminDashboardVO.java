package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 管理端仪表盘概览 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardVO {

    private Long userCount;

    private Long noteCount;

    private Long feedbackCount;

    private Long pendingFeedbackCount;

    private List<NoteTrendVO> trend;

    private List<HotCityVO> hotCities;

    private List<HotNoteVO> hotNotes;
}
