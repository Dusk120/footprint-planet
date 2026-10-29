package com.qzxx.footprint_planet.service;

import com.qzxx.footprint_planet.dto.request.PlanInviteDTO;
import com.qzxx.footprint_planet.vo.AcceptResultVO;
import com.qzxx.footprint_planet.vo.PlanInviteVO;

import java.util.List;

/** 计划邀请链接（签发 / 核销 / 撤销 / 列表） */
public interface PlanInviteService {

    /** 签发邀请链接（editor+ 可发），生成 token 并落 plan_invite */
    PlanInviteVO createInvite(Long planId, PlanInviteDTO dto);

    /** 核销邀请：校验 token 有效后写入 plan_collaborator（幂等），used_count+1 */
    AcceptResultVO acceptInvite(String token);

    /** 撤销邀请（仅 owner） */
    void revokeInvite(Long planId, Long inviteId);

    /** 查看某计划的邀请链接列表（仅 owner） */
    List<PlanInviteVO> listInvites(Long planId);
}
