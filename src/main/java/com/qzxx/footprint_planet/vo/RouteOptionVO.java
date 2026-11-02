package com.qzxx.footprint_planet.vo;

import lombok.Data;

/** 算路方案（返回给前端选择出行方案） */
@Data
public class RouteOptionVO {

    /** driving / walking / transit */
    private String mode;

    /** 驾车 / 步行 / 公交 */
    private String label;

    /** 时长（分钟） */
    private Integer duration;

    /** 距离（公里） */
    private Double distance;

    /** 方案摘要 */
    private String summary;

    /** 真实路线坐标串（画线用） */
    private String polyline;
}
