package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.RouteSearchDTO;
import com.qzxx.footprint_planet.service.RouteProxyService;
import com.qzxx.footprint_planet.vo.RouteOptionVO;
import com.qzxx.footprint_planet.vo.RouteResult;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 高德算路（Web 服务代理） */
@RestController
public class RouteController {

    @Resource
    private RouteProxyService routeProxyService;

    private static final Map<String, String> LABELS = Map.of(
            "driving", "驾车",
            "walking", "步行",
            "transit", "公交"
    );

    @PostMapping("/api/route/search")
    public Result<List<RouteOptionVO>> search(@RequestBody RouteSearchDTO dto) {
        String from = dto.getFromLng() + "," + dto.getFromLat();
        String to = dto.getToLng() + "," + dto.getToLat();
        List<RouteOptionVO> list = new ArrayList<>();
        for (String mode : new String[]{"driving", "walking", "transit"}) {
            RouteResult r = routeProxyService.calculateLeg(from, to, mode);
            if (r == null || r.getDuration() == null || r.getDistance() == null) continue;
            RouteOptionVO vo = new RouteOptionVO();
            vo.setMode(mode);
            vo.setLabel(LABELS.getOrDefault(mode, mode));
            vo.setDuration(Math.max(1, r.getDuration() / 60));          // 秒 -> 分钟
            vo.setDistance(Math.round(r.getDistance() / 100.0) / 10.0); // 米 -> 公里
            vo.setSummary(r.getSummary());
            vo.setPolyline(r.getPolyline());
            list.add(vo);
        }
        return Result.success(list);
    }
}
