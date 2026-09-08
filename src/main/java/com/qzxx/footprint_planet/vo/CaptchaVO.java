package com.qzxx.footprint_planet.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 图形验证码响应：uuid 供注册/登录请求体带回，img 为 dataURL（可直接作 <img src>） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO {

    private String uuid;

    private String img;
}
