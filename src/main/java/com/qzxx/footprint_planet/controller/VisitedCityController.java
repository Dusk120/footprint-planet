package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.VisitedCityDTO;
import com.qzxx.footprint_planet.entity.VisitedCity;
import com.qzxx.footprint_planet.service.VisitedCityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visited-cities")
public class VisitedCityController {
    @Autowired
    private VisitedCityService visitedCityService;

    @GetMapping
    public Result<List<VisitedCity>> getVisitedCities() {
        return Result.success(visitedCityService.getVisitedCities());
    }

    @PostMapping
    public Result<VisitedCity> addVisitedCity(@RequestBody @Valid VisitedCityDTO visitedCityDTO) {
        return Result.success(visitedCityService.addVisitedCity(visitedCityDTO));
    }

    @PutMapping("/{id}")
    public Result<VisitedCity> updateVisitedCity(@PathVariable Long id, @RequestBody @Valid VisitedCityDTO visitedCityDTO) {
        return Result.success(visitedCityService.updateVisitedCity(id, visitedCityDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteVisitedCity(@PathVariable Long id) {
        visitedCityService.deleteVisitedCity(id);
        return Result.success();
    }
}
