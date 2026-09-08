package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 近 7 天每日新增笔记数（日期 YYYY-MM-DD，升序） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteTrendVO {

    private String date;

    private Long noteCount;
}
