package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.VisitedCityDTO;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.vo.MemoryVO;
import java.util.List;

/** 足迹点亮城市（增删改查 / 统计） */
public interface VisitedCityService {
    List<VisitedCity> getVisitedCities();

    /** 旅行回忆：keyword 为空则返回全量，否则按城市名/省份模糊匹配 */
    List<MemoryVO> getMemories(String keyword);

    VisitedCity addVisitedCity(VisitedCityDTO visitedCityDTO);

    VisitedCity updateVisitedCity(Long id, VisitedCityDTO visitedCityDTO);

    void deleteVisitedCity(Long id);
}
