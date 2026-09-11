package com.qzxx.footprint_planet.service.impl;

import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.HotService;
import com.qzxx.footprint_planet.vo.HotCityVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotServiceImpl implements HotService {
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Override
    public List<HotCityVO> getHotDestinations() {
        return visitedCityMapper.selectHotCities();
    }
}
