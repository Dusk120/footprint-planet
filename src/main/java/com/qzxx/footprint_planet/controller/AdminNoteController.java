package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.AdminService;
import com.qzxx.footprint_planet.vo.AdminNoteVO;
import com.qzxx.footprint_planet.vo.PageVO;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/notes")
public class AdminNoteController {
    @Resource
    private AdminService adminService;

    @GetMapping
    public Result<PageVO<AdminNoteVO>> getNotes(@RequestParam(required = false) String keyword,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminService.getAdminNotes(keyword, page, pageSize));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        return Result.success(adminService.deleteNote(id));
    }
}
