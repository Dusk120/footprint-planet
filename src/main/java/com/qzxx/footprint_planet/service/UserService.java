package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.UpdateProfileDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.entity.User;
import com.qzxx.footprint_planet.vo.AuthorProfileVO;
import com.qzxx.footprint_planet.vo.PageVO;
import com.qzxx.footprint_planet.vo.UserVO;

/** 用户资料 / 作者主页 */
public interface UserService {
    UserVO updateMe(UpdateProfileDTO updateProfileDTO);

    AuthorProfileVO getAuthor(Long id);

    PageVO<Plan> getPlans(Long id, Integer page, Integer pageSize);

    /** 填充用户统计字段：点亮城市/省份实时 count，旅行里程/天数直接取自 user 表（已落库） */
    void fillUserStats(UserVO userVO, User user);

    /** 重算并落库用户的旅行里程/天数（登录、点亮城市的增删改后调用） */
    void recalculateAndSaveStats(Long userId);
}
