package com.qzxx.footprint_planet.vo;

import java.util.List;

/** 管理端仪表盘概览 */
public record AdminDashboardVO(
        Long userCount,
        Long noteCount,
        Long feedbackCount,
        Long pendingFeedbackCount,
        List<NoteTrendVO> trend,
        List<HotCityVO> hotCities,
        List<HotNoteVO> hotNotes
) {
}
