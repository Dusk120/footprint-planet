package com.qzxx.footprint_planet.vo;

import java.util.List;

/** 统一分页响应包装 */
public record PageVO<T>(
        List<T> list,
        long total,
        long page,
        long pageSize
) {
}
