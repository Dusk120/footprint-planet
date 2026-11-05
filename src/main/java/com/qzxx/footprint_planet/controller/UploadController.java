package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.RateLimiter;
import com.qzxx.footprint_planet.common.RedisKeys;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.OssService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final OssService ossService;
    @Resource
    private RateLimiter rateLimiter;

    public UploadController(OssService ossService) {
        this.ossService = ossService;
    }

    @PostMapping
    public Result<String> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        // required = false 是刻意的：若写成必填，缺参数时 Spring 抛 MissingServletRequestPartException，
        // 全局异常处理器没覆盖这个类型，会落到兜底的 Exception → 500「服务器开小差了」。
        // 声明为非必填后，缺参数走下面的分支，返回可读的 400。
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        if (!rateLimiter.tryAcquire(RedisKeys.UPLOAD_RATE_KEY, 60, 180)) {
            log.warn("上传被全局限流拦截, key={}", RedisKeys.UPLOAD_RATE_KEY);
            throw new BusinessException(429, "当前上传人数较多，请稍后重试");
        }
        try {
            return Result.success(ossService.upload(file));
        } catch (Exception e) {
            // 走到这里通常是 OSS 配置缺失（InvalidCredentialsException: Access key id should not be null or empty）
            // 或 OSS 网络异常。统一返回可识别的提示，不必让用户只看到「服务器开小差了」。
            // 完整堆栈写日志，便于运维定位到底是配置问题还是网络问题。
            log.error("图片上传失败", e);
            throw new BusinessException(503, "图片服务暂不可用，请稍后重试");
        }
    }
}
