package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.UpdateProfileDTO;
import com.qzxx.footprint_planet.entity.Plan;
import com.qzxx.footprint_planet.vo.AuthorProfileVO;
import com.qzxx.footprint_planet.vo.UserVO;

import java.util.List;

/** 用户资料 / 作者主页 */
public interface UserService {
    UserVO updateMe(UpdateProfileDTO updateProfileDTO);

    AuthorProfileVO getAuthor(Long id);

    List<Plan> getPlans(Long id);
}
