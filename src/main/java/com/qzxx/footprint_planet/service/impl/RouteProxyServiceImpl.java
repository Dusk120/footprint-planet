package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.service.RouteProxyService;
import com.qzxx.footprint_planet.vo.RouteResult;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RouteProxyServiceImpl implements RouteProxyService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public RouteResult calculateLeg(String fromCoord, String toCoord, String mode) {

        // TODO: 高德 key 到位后接真实 Web 服务 API + Redis 缓存（route:{from}:{to}:{mode}）
        RouteResult r = new RouteResult();
        switch (mode == null ? "driving" : mode) {
            case "walking":
                r.setDuration(1800); r.setDistance(1200);
                r.setSummary("步行约 30 分钟（mock）");
                break;
            case "riding":
                r.setDuration(900); r.setDistance(2500);
                r.setSummary("骑行约 15 分钟（mock）");
                break;
            case "transit":
                r.setDuration(2400); r.setDistance(10000);
                r.setSummary("公交约 40 分钟（mock）");
                break;
            default: // driving
                r.setDuration(1500); r.setDistance(8000);
                r.setSummary("驾车约 25 分钟（mock）");
        }
        r.setPolyline(fromCoord + ";" + toCoord);   // 临时格式：起终点两个坐标点连直线
        return r;
    }
    }
