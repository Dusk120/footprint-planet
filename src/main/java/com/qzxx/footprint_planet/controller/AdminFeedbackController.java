package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.UpdateFeedbackHandledDTO;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.vo.FeedbackVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/feedbacks")
public class AdminFeedbackController {
    @Resource
    private AdminService adminService;

    @GetMapping
    public Result<PageVO<FeedbackVO>> getFeedbacks(@RequestParam(required = false) Integer handled,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.getFeedbacks(handled, page, pageSize));
    }

    @PutMapping("/{id}/handled")
    public Result<Void> updateHandled(@PathVariable Long id, @RequestBody @Valid UpdateFeedbackHandledDTO updateFeedbackHandledDTO) {
        adminService.updateHandled(id, updateFeedbackHandledDTO.getHandled());
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        adminService.deleteFeedback(id);
        return Result.success();
    }
}
