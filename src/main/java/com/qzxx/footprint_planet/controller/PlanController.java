package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.service.PlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    @Autowired
    private PlanService planService;

    @GetMapping
    public Result<List<Plan>> getPlans() {
        return Result.success(planService.getPlans());
    }

    @PostMapping
    public Result<Plan> addPlan(@RequestBody @Valid PlanDTO planDTO) {
        return Result.success(planService.addPlan(planDTO));
    }

    @GetMapping("/{id}")
    public Result<Plan> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id));
    }

    @PutMapping("/{id}")
    public Result<Plan> updatePlan(@PathVariable Long id, @RequestBody @Valid PlanDTO planDTO) {
        return Result.success(planService.updatePlan(id, planDTO));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    public Result<Plan> copyPlan(@PathVariable Long id) {
        return Result.success(planService.copyPlan(id));
    }
}
