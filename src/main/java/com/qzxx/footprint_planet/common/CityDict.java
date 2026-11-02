package com.qzxx.footprint_planet.common;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 城市字典（adcode → 标准名 + 省份）。
 * 数据来自 src/main/resources/city-dict.json（由 scripts/generate-city-dict.js 从前端城市数据生成）。
 * 用途：点亮城市时校验 adcode 合法性 + 标准化 city_name / province；热门目的地按 adcode 反查标准名。
 */
@Component
public class CityDict {

    private final Map<String, Entry> dict = new HashMap<>();
    private final ObjectMapper objectMapper;

    public CityDict(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    void load() {
        try {
            String content = new String(
                    new ClassPathResource("city-dict.json").getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            Map<String, Object> raw = objectMapper.readValue(content, Map.class);
            raw.forEach((adcode, v) -> {
                Map<String, Object> entry = (Map<String, Object>) v;
                dict.put(adcode, new Entry(
                        String.valueOf(entry.get("name")),
                        String.valueOf(entry.get("province"))));
            });
        } catch (IOException e) {
            throw new IllegalStateException("加载城市字典 city-dict.json 失败", e);
        }
    }

    /** adcode 是否在字典中 */
    public boolean contains(String adcode) {
        return adcode != null && dict.containsKey(adcode);
    }

    /** 按 adcode 反查标准城市名；未知返回 null */
    public String name(String adcode) {
        Entry e = dict.get(adcode);
        return e == null ? null : e.name;
    }

    /** 按 adcode 反查省份；未知返回 null */
    public String province(String adcode) {
        Entry e = dict.get(adcode);
        return e == null ? null : e.province;
    }

    private record Entry(String name, String province) {}
}
