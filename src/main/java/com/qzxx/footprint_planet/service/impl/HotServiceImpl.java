package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.common.CityDict;
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
    @Resource
    private CityDict cityDict;


    @Override
    public List<HotCityVO> getHotDestinations() {
        Object cached = redisTemplate.opsForValue().get(RedisKeys.HOT_CITIES);
        if (cached instanceof List<?> list) {
            return list.stream().map(HotCityVO.class::cast).toList();
        }
        List<HotCityVO> hotCityVOS = visitedCityMapper.selectHotCities();
        // 统计按 adcode 分组，这里用字典反查标准城市名（避免「拉萨」vs「拉萨市」拆成两行）
        hotCityVOS.forEach(v -> v.setName(cityDict.name(v.getAdcode())));
        redisTemplate.opsForValue().set(RedisKeys.HOT_CITIES, hotCityVOS, 24, TimeUnit.HOURS);
        return hotCityVOS;
    }
}
