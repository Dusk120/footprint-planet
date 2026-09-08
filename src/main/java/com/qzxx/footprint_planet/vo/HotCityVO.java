package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 热门城市（社区 4.6 与仪表盘 5.1 共用口径） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotCityVO {

    private String name;

    private Long count;
}
