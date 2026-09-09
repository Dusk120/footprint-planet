package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.VisitedCityDTO;
import com.qzxx.footprint_planet.entity.VisitedCity;
import java.util.List;

/** 足迹点亮城市（增删改查 / 统计） */
public interface VisitedCityService {
    List<VisitedCity> getVisitedCities();

    VisitedCity addVisitedCity(VisitedCityDTO visitedCityDTO);

    VisitedCity updateVisitedCity(Long id, VisitedCityDTO visitedCityDTO);

    void deleteVisitedCity(Long id);
}
