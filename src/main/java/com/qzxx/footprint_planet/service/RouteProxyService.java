package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.vo.RouteResult;

/** 高德 Web 服务 API 代理层：统一封装路线计算，带 Redis 缓存降级（先 mock，key 到位后接真实 API） */
public interface RouteProxyService {

    /**
     * 计算单段路线。
     *
     * @param fromCoord 起点坐标（"lng,lat"）
     * @param toCoord   终点坐标（"lng,lat"）
     * @param mode      交通方式：driving / walking / transit
     */
    RouteResult calculateLeg(String fromCoord, String toCoord, String mode);
}
