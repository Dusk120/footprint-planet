package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.RedisKeys;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.VisitedCityDTO;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.VisitedCityService;
import com.qzxx.footprint_planet.vo.MemoryVO;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class VisitedCityServiceImpl implements VisitedCityService {
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public List<VisitedCity> getVisitedCities() {
        Long userId = BaseContext.getCurrentId();
        return visitedCityMapper.selectList(new QueryWrapper<VisitedCity>().eq("user_id", userId).orderByDesc("light_date").orderByDesc("id"));
    }

    @Override
    public List<MemoryVO> getMemories(String keyword) {
        Long userId = BaseContext.getCurrentId();
        String kw = keyword == null ? null : keyword.trim();
        List<VisitedCity> rows = visitedCityMapper.selectList(
                new LambdaQueryWrapper<VisitedCity>()
                        .eq(VisitedCity::getUserId, userId)
                        .and(StringUtils.hasText(kw), w -> w
                                .like(VisitedCity::getCityName, kw).or()
                                .like(VisitedCity::getProvince, kw)));
        List<MemoryVO> result = new ArrayList<>();
        for (VisitedCity row : rows) {
            List<Map<String, Object>> records = row.getRecords();
            if (records == null || records.isEmpty()) {
                continue;
            }
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> rec = records.get(i);
                if (rec == null) {
                    continue;
                }
                MemoryVO vo = new MemoryVO();
                vo.setCityId(row.getId());
                vo.setRecordIndex(i);
                vo.setCityName(row.getCityName());
                vo.setProvince(row.getProvince());
                vo.setAdcode(row.getAdcode());
                vo.setLat(row.getLat());
                vo.setLng(row.getLng());
                vo.setColor(row.getColor());
                vo.setLightDate(row.getLightDate());
                vo.setDate(resolveDate(asText(rec.get("date")), row.getLightDate()));
                vo.setRating(asRating(rec.get("rating")));
                vo.setPhotos(asPhotos(rec.get("photos")));
                result.add(vo);
            }
        }

        // 按游玩时间倒序；同城多条按 records 下标稳定排列
        result.sort(Comparator.comparing(MemoryVO::getDate).reversed()
                .thenComparing(MemoryVO::getCityId)
                .thenComparing(MemoryVO::getRecordIndex));
        return result;
    }

    @Override
    public VisitedCity addVisitedCity(VisitedCityDTO visitedCityDTO) {
        int count = visitedCityMapper.selectCount(new QueryWrapper<VisitedCity>().eq("user_id", BaseContext.getCurrentId()).eq("city_name", visitedCityDTO.getCityName())).intValue();
        if (count > 0) {
            throw new BusinessException(400, "该城市已存在");
        }
        if (visitedCityDTO.getLightDate() == null) {
            visitedCityDTO.setLightDate(LocalDate.now());
        }
        VisitedCity visitedCity = new VisitedCity();
        BeanUtil.copyProperties(visitedCityDTO, visitedCity);
        visitedCity.setUserId(BaseContext.getCurrentId());
        visitedCity.setCreatedAt(LocalDateTime.now());
        // 未填游玩时间的记录按本次点亮日期兜底（完全空的记录保留）
        fillRecordDates(visitedCity.getRecords(), visitedCity.getLightDate());
        visitedCityMapper.insert(visitedCity);
        redisTemplate.delete(RedisKeys.HOT_CITIES);
        return visitedCity;
    }

    @Override
    public VisitedCity updateVisitedCity(Long id, VisitedCityDTO visitedCityDTO) {
        VisitedCity visitedCity = visitedCityMapper.selectById(id);
        if (visitedCity == null) {
            throw new BusinessException(404, "记录不存在");
        }
        if (!visitedCity.getUserId().equals(BaseContext.getCurrentId())) {
            throw new BusinessException(403, "无权限修改");
        }
        // 只覆盖前端实际传了的字段，避免把未传的 light_date 等清空
        BeanUtil.copyProperties(visitedCityDTO, visitedCity, CopyOptions.create().ignoreNullValue().setIgnoreProperties("cityName"));
        if (visitedCity.getLightDate() == null) {
            visitedCity.setLightDate(LocalDate.now());
        }
        fillRecordDates(visitedCity.getRecords(), visitedCity.getLightDate());
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
        redisTemplate.delete(RedisKeys.HOT_CITIES);
        visitedCityMapper.deleteById(id);
    }

    /** 游玩时间兜底：记录里的 date 为空 → 该城市点亮日期 → 当天 */
    private String resolveDate(String date, LocalDate lightDate) {
        if (date != null && !date.isBlank()) {
            return date;
        }
        return (lightDate != null ? lightDate : LocalDate.now()).toString();
    }

    /** 给 records 里每条记录的 date 兜底（就地修改，空记录保留） */
    private void fillRecordDates(List<Map<String, Object>> records, LocalDate fallback) {
        if (records == null) {
            return;
        }
        LocalDate base = fallback != null ? fallback : LocalDate.now();
        for (Map<String, Object> rec : records) {
            if (rec == null) {
                continue;
            }
            if (asText(rec.get("date")) == null) {
                rec.put("date", base.toString());
            }
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }

    private Integer asRating(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<String> asPhotos(Object value) {
        List<String> photos = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    photos.add(String.valueOf(o));
                }
            }
        }
        return photos;
    }
}
