package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.OssService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final OssService ossService;

    public UploadController(OssService ossService) {
        this.ossService = ossService;
    }

    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }
        return Result.success(ossService.upload(file));
    }
}
