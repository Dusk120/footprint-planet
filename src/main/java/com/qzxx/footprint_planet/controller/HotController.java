package com.qzxx.footprint_planet.controller;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.HotService;
import com.qzxx.footprint_planet.vo.HotCityVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hot-destinations")
public class HotController {
    @Resource
    private HotService hotService;

    @GetMapping
    public Result<List<HotCityVO>> getHotDestinations() {
        return Result.success(hotService.getHotDestinations());
    }
}
