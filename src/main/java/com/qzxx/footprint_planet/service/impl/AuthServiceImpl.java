package com.qzxx.footprint_planet.service.impl;


import jakarta.annotation.Resource;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.config.JwtProperties;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.LoginDTO;
import com.qzxx.footprint_planet.dto.request.RegisterDTO;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.service.AuthService;
import com.qzxx.footprint_planet.service.CaptchaService;
import com.qzxx.footprint_planet.service.UserService;
import com.qzxx.footprint_planet.utils.JwtUtil;
import com.qzxx.footprint_planet.vo.LoginVO;
import com.qzxx.footprint_planet.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private JwtProperties jwtProperties;
    @Resource
    private CaptchaService captchaService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserService userService;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        captchaService.verify(loginDTO.getUuid(), loginDTO.getCode());
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", loginDTO.getUsername()));
        if (user == null) {
            throw new BusinessException(400,"用户名或密码错误");
        }
        boolean match = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword());
        if (!match) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (user.getStatus() == 1) {
            throw new BusinessException(400,"账号已被禁用");
        }
        // 登录时重算旅行里程/天数并落库，保证读到最新值
        userService.recalculateAndSaveStats(user.getId());
        user = userMapper.selectById(user.getId());
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userService.fillUserStats(userVO, user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        return new LoginVO(token, userVO);
    }

    @Override
    public LoginVO register(RegisterDTO registerDTO) {
        captchaService.verify(registerDTO.getUuid(), registerDTO.getCode());
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", registerDTO.getUsername())) != null) {
            throw new BusinessException(400, "用户名已存在");
        }
        String password = registerDTO.getPassword();
        String encryptPwd = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(encryptPwd);
        if (registerDTO.getNickname() != null) {
            user.setNickname(registerDTO.getNickname());
        }
        else {
            user.setNickname("user_" + UUID.randomUUID().toString().substring(0, 7));
        }
        user.setRole("USER");
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setTotalKm(0);
        user.setMotto("");
        userMapper.insert(user);
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userService.fillUserStats(userVO, user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecretKey(),
                jwtProperties.getTtl(),
                claims);
        return new LoginVO(token, userVO);
    }

    @Override
    public void logout() {
    }

    @Override
    public UserVO me() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userService.fillUserStats(userVO, user);
        return userVO;
    }
}
