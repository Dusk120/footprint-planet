package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.CaptchaService;
import com.qzxx.footprint_planet.vo.CaptchaVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping
    public Result<CaptchaVO> captcha() {
        return Result.success(captchaService.create());
    }
}
