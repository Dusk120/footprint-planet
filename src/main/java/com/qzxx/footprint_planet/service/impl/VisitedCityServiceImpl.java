package com.qzxx.footprint_planet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.VisitedCityDTO;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.VisitedCityService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class VisitedCityServiceImpl implements VisitedCityService {
    @Resource
    private VisitedCityMapper visitedCityMapper;

    @Override
    public List<VisitedCity> getVisitedCities() {
        Long userId = BaseContext.getCurrentId();
        return visitedCityMapper.selectList(new QueryWrapper<VisitedCity>().eq("user_id", userId));
    }

    @Override
    public VisitedCity addVisitedCity(VisitedCityDTO visitedCityDTO) {
        int count = visitedCityMapper.selectCount(new QueryWrapper<VisitedCity>().eq("user_id", BaseContext.getCurrentId()).eq("city_name", visitedCityDTO.getCityName())).intValue();
        if (count > 0) {
            throw new BusinessException(400, "该城市已存在");
        }
        if(visitedCityDTO.getLightDate()==null){
            visitedCityDTO.setLightDate(java.time.LocalDate.now());
        }
        VisitedCity visitedCity = new VisitedCity();
        BeanUtils.copyProperties(visitedCityDTO, visitedCity);
        visitedCity.setUserId(BaseContext.getCurrentId());
        visitedCity.setCreatedAt(java.time.LocalDateTime.now());
        visitedCityMapper.insert(visitedCity);
        return visitedCity;
    }

    @Override
    public VisitedCity updateVisitedCity(Long id, VisitedCityDTO visitedCityDTO) {
        VisitedCity visitedCity = visitedCityMapper.selectById(id);
        if (visitedCity == null ) {
            throw new BusinessException(404,"记录不存在");
        }
        if (!visitedCity.getUserId().equals(BaseContext.getCurrentId())) {
            throw new BusinessException(403,"无权限修改");
        }
        BeanUtils.copyProperties(visitedCityDTO, visitedCity);
        visitedCityMapper.updateById(visitedCity);
        return visitedCity;
    }

    @Override
    public void deleteVisitedCity(Long id) {
        VisitedCity visitedCity = visitedCityMapper.selectById(id);
        if (visitedCity == null) {
            throw new BusinessException(404, "记录不存在");
        }
        if (!visitedCity.getUserId().equals(BaseContext.getCurrentId())) {
            throw new BusinessException(403, "无权限删除");
        }
            visitedCityMapper.deleteById(id);
    }
}
