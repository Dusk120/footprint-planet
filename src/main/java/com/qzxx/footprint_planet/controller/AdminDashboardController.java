package com.qzxx.footprint_planet.controller;


import jakarta.annotation.Resource;
import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.vo.AdminDashboardVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    @Resource
    private AdminService adminService;

    @GetMapping("/overview")
    public Result<AdminDashboardVO> getOverview() {
        return Result.success(adminService.getAdminDashboard());
    }
}
