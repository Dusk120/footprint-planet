package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.vo.HotCityVO;

import java.util.List;

/** 热门目的地（由 visited_city 实时统计） */
public interface HotService {

    List<HotCityVO> getHotDestinations();

}
