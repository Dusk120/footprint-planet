package com.qzxx.footprint_planet.dto.request;

import lombok.Data;

/** 后端算路请求：起终点坐标 */
@Data
public class RouteSearchDTO {

    private Double fromLng;

    private Double fromLat;

    private Double toLng;

    private Double toLat;
}
