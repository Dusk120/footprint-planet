package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.ResetPasswordDTO;
import com.qzxx.footprint_planet.dto.request.UpdateUserStatusDTO;
import com.qzxx.footprint_planet.vo.AdminDashboardVO;
import com.qzxx.footprint_planet.vo.AdminUserDetailVO;
import com.qzxx.footprint_planet.vo.AdminUserVO;
import com.qzxx.footprint_planet.vo.PageVO;

/** 管理端：仪表盘 / 用户 / 笔记 / 反馈 */
public interface AdminService {
    AdminDashboardVO getAdminDashboard();


    PageVO<AdminUserVO> getUsers(String keyword, int page, int pageSize);

    AdminUserDetailVO getUser(Long id);

    Void updateStatus(Long id, UpdateUserStatusDTO updateUserStatusDTO);

    Void updatePassword(Long id, ResetPasswordDTO resetPasswordDTO);
}
