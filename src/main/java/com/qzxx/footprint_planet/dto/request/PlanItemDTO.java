package com.qzxx.footprint_planet.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanItemDTO {

    @NotBlank(message = "地点名称不能为空")
    private String name;

    /** 第几天（从 1 开始） */
    private Integer dayNo;

    /** 当天顺序 */
    private Integer sortNo;

    /** 经度（GCJ-02） */
    private Double lng;

    /** 纬度（GCJ-02） */
    private Double lat;

    /** 高德 POI ID（预留） */
    private String poiId;

    private String note;
}
