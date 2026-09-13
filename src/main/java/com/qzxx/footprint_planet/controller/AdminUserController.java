package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.ResetPasswordDTO;
import com.qzxx.footprint_planet.dto.request.UpdateUserStatusDTO;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.vo.AdminUserDetailVO;
import com.qzxx.footprint_planet.vo.AdminUserVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    @Autowired
    private AdminService adminService;

    @GetMapping
    public Result<PageVO<AdminUserVO>> getUsers(String keyword, int page, int pageSize) {
        return Result.success(adminService.getUsers(keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<AdminUserDetailVO> getUser(@PathVariable Long id) {
        return Result.success(adminService.getUser(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody @Valid UpdateUserStatusDTO updateUserStatusDTO) {
        return Result.success(adminService.updateStatus(id, updateUserStatusDTO));
    }

    @PutMapping("/{id}/password")
    public Result<Void> updatePassword(@PathVariable Long id, @RequestBody @Valid ResetPasswordDTO resetPasswordDTO) {
        return Result.success(adminService.updatePassword(id, resetPasswordDTO));
    }
}
