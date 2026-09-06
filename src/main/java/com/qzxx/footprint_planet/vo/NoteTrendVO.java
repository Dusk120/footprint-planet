package com.qzxx.footprint_planet.vo;

/** 近 7 天每日新增笔记数（日期 YYYY-MM-DD，升序） */
public record NoteTrendVO(
        String date,
        Long noteCount
) {
}
