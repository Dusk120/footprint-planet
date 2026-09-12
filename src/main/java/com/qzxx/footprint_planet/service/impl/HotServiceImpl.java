package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.HotService;
import com.qzxx.footprint_planet.vo.HotCityVO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class HotServiceImpl implements HotService {
    private static final String KEY = "hot_cities";
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public List<HotCityVO> getHotDestinations() {
        Object cached = redisTemplate.opsForValue().get(KEY);
        if (cached instanceof List<?> list) {
            return list.stream().map(HotCityVO.class::cast).toList();
        }
        List<HotCityVO> hotCityVOS = visitedCityMapper.selectHotCities();
        redisTemplate.opsForValue().set(KEY, hotCityVOS, 24, TimeUnit.HOURS);
        return hotCityVOS;
    }
}
