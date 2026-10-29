package com.qzxx.footprint_planet.controller;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.DeadlineDTO;
import com.qzxx.footprint_planet.dto.request.LockDTO;
import com.qzxx.footprint_planet.dto.request.PlanDTO;
import com.qzxx.footprint_planet.dto.request.PlanItemDTO;
import com.qzxx.footprint_planet.dto.request.PlanLegDTO;
import com.qzxx.footprint_planet.dto.request.ReorderDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.entity.PlanItem;
import com.qzxx.footprint_planet.entity.PlanLeg;
import com.qzxx.footprint_planet.service.PlanService;
import com.qzxx.footprint_planet.vo.PageVO;
import com.qzxx.footprint_planet.vo.PlanDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController {
    @Resource
    private PlanService planService;

    @GetMapping
    public Result<PageVO<Plan>> getPlans(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(planService.getPlans(page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<PlanDetailVO> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id));
    }

    @PostMapping
    public Result<Plan> addPlan(@RequestBody @Valid PlanDTO planDTO) {
        return Result.success(planService.addPlan(planDTO));
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

    @PostMapping("/{id}/cover")
    public Result<Void> setCover(@PathVariable Long id, @RequestBody Map<String, String> body) {
        planService.setCover(id, body.get("image"));
        return Result.success();
    }

    @PostMapping("/{id}/items")
    public Result<PlanItem> addItem(@PathVariable Long id, @RequestBody @Valid PlanItemDTO dto) {
        return Result.success(planService.addItem(id, dto));
    }

    @PutMapping("/{id}/items/{itemId}")
    public Result<PlanItem> updateItem(@PathVariable Long id, @PathVariable Long itemId,
                                       @RequestBody @Valid PlanItemDTO dto) {
        return Result.success(planService.updateItem(id, itemId, dto));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        planService.deleteItem(id, itemId);
        return Result.success();
    }

    @PutMapping("/{id}/items/reorder")
    public Result<Void> reorderItems(@PathVariable Long id, @RequestBody @Valid ReorderDTO dto) {
        planService.reorderItems(id, dto);
        return Result.success();
    }

    @PostMapping("/{id}/legs")
    public Result<PlanLeg> saveLeg(@PathVariable Long id, @RequestBody @Valid PlanLegDTO dto) {
        return Result.success(planService.saveLeg(id, dto));
    }

    @PutMapping("/{id}/lock")
    public Result<Void> setCollabLocked(@PathVariable Long id, @RequestBody @Valid LockDTO dto) {
        planService.setCollabLocked(id, dto.getLocked());
        return Result.success();
    }

    @PutMapping("/{id}/deadline")
    public Result<Void> setCollabDeadline(@PathVariable Long id, @RequestBody DeadlineDTO dto) {
        planService.setCollabDeadline(id, dto.getDeadline());
        return Result.success();
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public Result<Void> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        planService.removeCollaborator(id, userId);
        return Result.success();
    }
}
