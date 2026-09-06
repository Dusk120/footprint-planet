package com.qzxx.footprint_planet.vo;

/** 笔记关联地点（响应嵌套用；无关联地点时整体为 null） */
public record LocationVO(
        String name,
        String province
) {
}
