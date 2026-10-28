package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.service.NotificationsService;
import com.qzxx.footprint_planet.vo.NotificationPageVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notifications")
public class NotificationsController {
    @Resource
    private NotificationsService notificationService;

    @GetMapping
    public Result<NotificationPageVO> getNotifications(@RequestParam String type,
                                                       @RequestParam(required = false) Long cursor,
                                                       @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(notificationService.getNotifications(type, cursor, pageSize));
    }

    @PutMapping("/{id}/read")
    public Result<Void> readNotification(@PathVariable Long id) {
        return Result.success(notificationService.readNotification(id));
    }

    @PostMapping("/read-all")
    public Result<Void> readAllNotifications(@RequestParam String type) {
        return Result.success(notificationService.readAllNotifications(type));
    }
}
