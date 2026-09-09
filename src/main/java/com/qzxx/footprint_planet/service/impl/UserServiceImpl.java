package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.UpdateProfileDTO;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.UserService;
import com.qzxx.footprint_planet.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private VisitedCityMapper visitedCityMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public UserVO updateMe(UpdateProfileDTO updateProfileDTO) {
        boolean changed = false;
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (updateProfileDTO.getNickname() != null && !updateProfileDTO.getNickname().isEmpty()) {
            user.setNickname(updateProfileDTO.getNickname());
            changed = true;
        }
        if (updateProfileDTO.getOldPassword() != null && !updateProfileDTO.getOldPassword().isEmpty()) {
            boolean match = passwordEncoder.matches(updateProfileDTO.getOldPassword(), user.getPassword());
            if (!match) {
                throw new BusinessException(400, "旧密码错误");
            }
            if (updateProfileDTO.getNewPassword() != null && !updateProfileDTO.getNewPassword().isEmpty()) {
                if (updateProfileDTO.getNewPassword().length() < 6 || updateProfileDTO.getNewPassword().length() > 50) {
                    throw new BusinessException(400, "新密码长度不能小于 6 位或大于 50 位");
                }
                if (passwordEncoder.matches(updateProfileDTO.getNewPassword(), user.getPassword())) {
                    throw new BusinessException(400, "新密码不能与旧密码相同");
                }
                user.setPassword(passwordEncoder.encode(updateProfileDTO.getNewPassword()));
                changed = true;
            }
        }
        else if (updateProfileDTO.getNewPassword() != null && !updateProfileDTO.getNewPassword().isEmpty()) {
            throw new BusinessException(400, "请输入旧密码");
        }
        if (updateProfileDTO.getMotto() != null ) {
            user.setMotto(updateProfileDTO.getMotto());
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        userVO.setVisitedCities(visitedCityMapper.countCitiesByUserId(userId));
        userVO.setVisitedProvinces(visitedCityMapper.countProvincesByUserId(userId));
        return userVO;
    }
}
