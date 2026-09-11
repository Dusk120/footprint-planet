package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.UpdateProfileDTO;
import com.qzxx.footprint_planet.entity.Note;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.mapper.NoteMapper;
import com.qzxx.footprint_planet.mapper.PlanMapper;
import com.qzxx.footprint_planet.mapper.UserMapper;
import com.qzxx.footprint_planet.mapper.VisitedCityMapper;
import com.qzxx.footprint_planet.service.UserService;
import com.qzxx.footprint_planet.vo.AuthorProfileVO;
import com.qzxx.footprint_planet.vo.PageVO;
import com.qzxx.footprint_planet.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private VisitedCityMapper visitedCityMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private NoteMapper noteMapper;
    @Resource
    private PlanMapper planMapper;


    @Override
    @Transactional
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
            noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                    .eq(Note::getAuthorId, user.getId())
                    .set(Note::getAuthorNickname, updateProfileDTO.getNickname()));
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
        if (updateProfileDTO.getAvatar() != null && !updateProfileDTO.getAvatar().isEmpty()) {
            user.setAvatar(updateProfileDTO.getAvatar());
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

    @Override
    public AuthorProfileVO getAuthor(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "作者不存在");
        }
        AuthorProfileVO authorProfileVO = new AuthorProfileVO();
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        authorProfileVO.setUser(userVO);
        userVO.setVisitedCities(visitedCityMapper.countCitiesByUserId(user.getId()));
        userVO.setVisitedProvinces(visitedCityMapper.countProvincesByUserId(user.getId()));
        authorProfileVO.setNotesCount(noteMapper.selectCount(new LambdaQueryWrapper<Note>().eq(Note::getAuthorId, user.getId())).intValue());
        authorProfileVO.setPlansCount(planMapper.selectCount(new LambdaQueryWrapper<Plan>().eq(Plan::getUserId, user.getId()).eq(Plan::getIsPublic, true)).intValue());
        return authorProfileVO;
    }

    @Override
    public PageVO<Plan> getPlans(Long id, Integer page, Integer pageSize) {
        Page<Plan> p = new Page<>(page, pageSize);
        p.addOrder(OrderItem.desc("created_at"));
        Page<Plan> result = planMapper.selectPage(p, new LambdaQueryWrapper<Plan>()
                .eq(Plan::getUserId, id)
                .eq(Plan::getIsPublic, true));
        return new PageVO<>(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }
}
