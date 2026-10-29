package com.qzxx.footprint_planet.vo;

import lombok.Data;

/** 高德路线计算结果（Web 服务代理层返回，先 mock 后接真实 API） */
@Data
public class RouteResult {

    /** 时长（秒） */
    private Integer duration;

    /** 距离（米） */
    private Integer distance;

    /** 方案摘要 */
    private String summary;

    /** 简化路线坐标串 */
    private String polyline;
}
