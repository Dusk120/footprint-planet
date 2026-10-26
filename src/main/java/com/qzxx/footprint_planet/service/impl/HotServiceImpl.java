package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.common.RedisKeys;
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
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public List<HotCityVO> getHotDestinations() {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.HOT_CITIES);
        if (cached instanceof List<?> list) {
            return list.stream().map(HotCityVO.class::cast).toList();
        }
        List<HotCityVO> hotCityVOS = visitedCityMapper.selectHotCities();
        redisTemplate.opsForValue().set(RedisKeys.HOT_CITIES, hotCityVOS, 24, TimeUnit.HOURS);
        return hotCityVOS;
    }
}
