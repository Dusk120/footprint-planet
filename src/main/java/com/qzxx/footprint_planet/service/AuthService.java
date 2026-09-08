package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.LoginDTO;
import com.qzxx.footprint_planet.dto.request.RegisterDTO;
import com.qzxx.footprint_planet.vo.LoginVO;
import com.qzxx.footprint_planet.vo.UserVO;

/** 认证：注册 / 登录 / 退出 / 当前用户 */
public interface AuthService {

    LoginVO login(LoginDTO loginDTO);

    LoginVO register(RegisterDTO registerDTO);

    void logout();

    UserVO me();
}
