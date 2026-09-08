package com.qzxx.footprint_planet.controller;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.LoginDTO;
import com.qzxx.footprint_planet.dto.request.RegisterDTO;
import com.qzxx.footprint_planet.service.AuthService;
import com.qzxx.footprint_planet.vo.LoginVO;
import com.qzxx.footprint_planet.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证接口：注册 / 登录 / 退出 / 当前用户（register、login 已入拦截器白名单，me 需登录） */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Valid RegisterDTO registerDTO) {
        return Result.success(authService.register(registerDTO));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.me());
    }
}
