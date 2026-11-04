package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.RateLimiter;
import com.qzxx.footprint_planet.common.RedisKeys;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.context.BaseContext;
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
import java.util.Locale;
import java.util.Map;

/** 高德算路（Web 服务代理） */
@RestController
public class RouteController {

    @Resource
    private RouteProxyService routeProxyService;
    @Resource
    private RateLimiter rateLimiter;

    private static final Map<String, String> LABELS = Map.of(
            "driving", "驾车",
            "walking", "步行",
            "transit", "公交"
    );

    @PostMapping("/api/route/search")
    public Result<List<RouteOptionVO>> search(@RequestBody RouteSearchDTO dto) {
        if (!rateLimiter.tryAcquire(RedisKeys.routeRateLimit(BaseContext.getCurrentId()), 60, 30)) {
            throw new BusinessException(429, "操作太频繁，请稍后再试");
        }

        String from = coord(dto.getFromLng(), dto.getFromLat(), "起点");
        String to = coord(dto.getToLng(), dto.getToLat(), "终点");
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

    /**
     * 校验坐标并格式化成高德要求的 "lng,lat"。
     *
     * <p>三件事合在一处做：
     * <ol>
     *   <li>非空校验：少了坐标就抛出 400，避免把 "null,null" 发给高德白烧调用；</li>
     *   <li>范围校验：经度 ±180、纬度 ±90；</li>
     *   <li>定点格式化：Double.toString 在极小值时会输出科学计数法（如 1.0E-4），
     *       高德解析不了，还会让缓存 key 变成另一套写法无法命中。
     *       必须指定 Locale.ROOT，否则某些区域设置下小数点是逗号，会直接破坏 "lng,lat" 结构。</li>
     * </ol>
     */
    private static String coord(Double lng, Double lat, String label) {
        if (lng == null || lat == null) {
            throw new BusinessException(400, label + "坐标不能为空");
        }
        if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
            throw new BusinessException(400, label + "坐标超出有效范围");
        }
        return String.format(Locale.ROOT, "%.6f,%.6f", lng, lat);
    }
}
