package com.qzxx.footprint_planet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qzxx.footprint_planet.entity.VisitedCity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface VisitedCityMapper extends BaseMapper<VisitedCity> {

    @Select("SELECT COUNT(DISTINCT city_name) FROM visited_city WHERE user_id = #{userId}")
    Integer countCitiesByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT province) FROM visited_city WHERE user_id = #{userId}")
    Integer countProvincesByUserId(@Param("userId") Long userId);

}
