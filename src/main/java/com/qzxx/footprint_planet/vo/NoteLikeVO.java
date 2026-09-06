package com.qzxx.footprint_planet.vo;

/** 点赞 / 取消点赞操作后的结果 */
public record NoteLikeVO(
        Integer likes,
        Boolean liked
) {
}
