package com.qzxx.footprint_planet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.vo.HotCityVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface VisitedCityMapper extends BaseMapper<VisitedCity> {

    @Select("SELECT COUNT(DISTINCT adcode) FROM visited_city WHERE user_id = #{userId}")
    Integer countCitiesByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT province) FROM visited_city WHERE user_id = #{userId}")
    Integer countProvincesByUserId(@Param("userId") Long userId);

    @Select("select adcode, count(*) as count from visited_city where adcode is not null group by adcode order by count desc, adcode asc limit 5")
    List<HotCityVO> selectHotCities();
}
