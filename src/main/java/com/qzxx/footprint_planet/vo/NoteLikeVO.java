package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 点赞 / 取消点赞操作后的结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteLikeVO {

    private Integer likes;

    private Boolean liked;
}
