package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.UpdateProfileDTO;
import com.qzxx.footprint_planet.service.UserService;
import com.qzxx.footprint_planet.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @PutMapping("/me")
    public Result<UserVO> updateMe(@RequestBody @Valid UpdateProfileDTO updateProfileDTO) {
        return Result.success(userService.updateMe(updateProfileDTO));
    }
}
