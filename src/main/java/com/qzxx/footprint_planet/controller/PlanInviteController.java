package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.PlanInviteDTO;
import com.qzxx.footprint_planet.dto.request.PlanInviteUpdateDTO;
import com.qzxx.footprint_planet.service.PlanInviteService;
import com.qzxx.footprint_planet.vo.AcceptResultVO;
import com.qzxx.footprint_planet.vo.PlanInviteVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlanInviteController {

    @Resource
    private PlanInviteService planInviteService;

    @PostMapping("/api/plans/{planId}/invites")
    public Result<PlanInviteVO> createInvite(@PathVariable Long planId,
                                             @RequestBody @Valid PlanInviteDTO dto) {
        return Result.success(planInviteService.createInvite(planId, dto));
    }

    @PostMapping("/api/invites/{token}/accept")
    public Result<AcceptResultVO> acceptInvite(@PathVariable String token) {
        return Result.success(planInviteService.acceptInvite(token));
    }

    @DeleteMapping("/api/plans/{planId}/invites/{inviteId}")
    public Result<Void> revokeInvite(@PathVariable Long planId, @PathVariable Long inviteId) {
        planInviteService.revokeInvite(planId, inviteId);
        return Result.success();
    }

    @PutMapping("/api/plans/{planId}/invites/{inviteId}")
    public Result<PlanInviteVO> updateInvite(@PathVariable Long planId,
                                             @PathVariable Long inviteId,
                                             @RequestBody @Valid PlanInviteUpdateDTO dto) {
        return Result.success(planInviteService.updateInvite(planId, inviteId, dto));
    }

    @GetMapping("/api/plans/{planId}/invites")
    public Result<List<PlanInviteVO>> listInvites(@PathVariable Long planId) {
        return Result.success(planInviteService.listInvites(planId));
    }
}
