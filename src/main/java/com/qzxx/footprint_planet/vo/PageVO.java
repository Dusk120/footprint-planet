package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 统一分页响应包装 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    private List<T> list;

    private long total;

    private long page;

    private long pageSize;
}
