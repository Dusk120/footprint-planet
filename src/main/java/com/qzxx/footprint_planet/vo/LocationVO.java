package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 笔记关联地点（响应嵌套用；无关联地点时整体为 null） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationVO {

    private String name;

    private String province;
}
