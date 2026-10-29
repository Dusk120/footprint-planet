package com.qzxx.footprint_planet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("plan_item")
public class PlanItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    /** 第几天（从 1 开始） */
    private Integer dayNo;

    /** 当天顺序 */
    private Integer sortNo;

    /** 地点名称（高德 POI 正规名称） */
    private String name;

    /** 经度（GCJ-02） */
    private Double lng;

    /** 纬度（GCJ-02） */
    private Double lat;

    /** 高德 POI ID */
    private String poiId;

    /** 用户备注 */
    private String note;

    private LocalDateTime createdAt;
}
