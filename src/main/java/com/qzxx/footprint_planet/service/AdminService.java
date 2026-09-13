package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.common.Result;
import com.qzxx.footprint_planet.dto.request.ResetPasswordDTO;
import com.qzxx.footprint_planet.dto.request.UpdateUserStatusDTO;
import com.qzxx.footprint_planet.entity.Feedback;
import com.qzxx.footprint_planet.vo.*;

/** 管理端：仪表盘 / 用户 / 笔记 / 反馈 */
public interface AdminService {
    AdminDashboardVO getAdminDashboard();


    PageVO<AdminUserVO> getUsers(String keyword, int page, int pageSize);

    AdminUserDetailVO getUser(Long id);

    Void updateStatus(Long id, UpdateUserStatusDTO updateUserStatusDTO);

    Void updatePassword(Long id, ResetPasswordDTO resetPasswordDTO);

    PageVO<AdminNoteVO> getAdminNotes(String keyword, int page, int pageSize);

    Void deleteNote(Long id);

    PageVO<FeedbackVO> getFeedbacks(Integer handled, int page, int pageSize);

    void updateHandled(Long id, boolean handled);

    void deleteFeedback(Long id);
}
