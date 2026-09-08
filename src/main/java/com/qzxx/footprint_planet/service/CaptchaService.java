package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.vo.CaptchaVO;

/** 图形验证码：生成并缓存 / 校验（一次性） */
public interface CaptchaService {

    /** 生成验证码并存入 Redis，返回编号与 base64 图片 */
    CaptchaVO create();

    /** 校验验证码：错误 / 过期抛业务异常，通过后立即作废（一次性） */
    void verify(String uuid, String code);
}
