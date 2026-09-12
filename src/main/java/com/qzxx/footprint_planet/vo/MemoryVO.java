package com.qzxx.footprint_planet.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 旅行回忆条目：visited_city.records 数组展平后的一条游玩记录 */
@Data
public class MemoryVO {

    /** 所属点亮城市 ID（删除单条记录时回写用） */
    private Long cityId;

    /** 该记录在 records 数组中的下标，配合 cityId 唯一定位一条回忆 */
    private Integer recordIndex;

    private String cityName;

    private String province;

    private String adcode;

    private Double lat;

    private Double lng;

    private String color;

    /** 点亮日期 */
    private LocalDate lightDate;

    /** 游玩时间 YYYY-MM-DD（为空时后端以点亮日期兜底，保证非空） */
    private String date;

    private Integer rating;

    private List<String> photos;
}
