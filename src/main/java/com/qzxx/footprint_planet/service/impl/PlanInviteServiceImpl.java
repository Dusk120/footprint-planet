package com.qzxx.footprint_planet.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qzxx.footprint_planet.common.BusinessException;
import com.qzxx.footprint_planet.common.PlanRole;
import com.qzxx.footprint_planet.context.BaseContext;
import com.qzxx.footprint_planet.dto.request.PlanInviteDTO;
import com.qzxx.footprint_planet.dto.request.PlanInviteUpdateDTO;
import com.qzxx.footprint_planet.entity.PlanCollaborator;
import com.qzxx.footprint_planet.entity.PlanInvite;
import com.qzxx.footprint_planet.mapper.PlanCollaboratorMapper;
import com.qzxx.footprint_planet.mapper.PlanInviteMapper;
import com.qzxx.footprint_planet.mapper.PlanMapper;
import com.qzxx.footprint_planet.service.PlanInviteService;
import com.qzxx.footprint_planet.service.PlanService;
import com.qzxx.footprint_planet.vo.AcceptResultVO;
import com.qzxx.footprint_planet.vo.PlanInviteVO;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PlanInviteServiceImpl implements PlanInviteService {
    @Value("${app.frontend-url}")
    private String frontendBaseUrl;

    @Resource
    private PlanInviteMapper planInviteMapper;
    @Resource
    private PlanCollaboratorMapper planCollaboratorMapper;
    @Resource
    private PlanMapper planMapper;
    @Resource
    private PlanService planService;

    @Override
    public PlanInviteVO createInvite(Long planId, PlanInviteDTO dto) {
        planService.requireOwner(planId);
        PlanInvite invite = new PlanInvite();
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setPlanId(planId);
        invite.setRole(dto.getRole());
        invite.setExpiresAt(dto.getExpiresAt());
        invite.setMaxUses(dto.getMaxUses());
        invite.setUsedCount(0);
        invite.setRevoked(false);
        invite.setCreatedAt(LocalDateTime.now());
        invite.setCreatedBy(BaseContext.getCurrentId());
        planInviteMapper.insert(invite);

        PlanInviteVO vo = BeanUtil.copyProperties(invite, PlanInviteVO.class);
        vo.setLink( frontendBaseUrl + "/invite/" + invite.getToken());
        return vo;
    }

    @Transactional
    @Override
    public AcceptResultVO acceptInvite(String token) {
        PlanInvite invite = planInviteMapper.selectOne(
                new LambdaQueryWrapper<PlanInvite>().eq(PlanInvite::getToken, token));
        if (invite == null) throw new BusinessException(404, "邀请链接无效");
        if (Boolean.TRUE.equals(invite.getRevoked()))
            throw new BusinessException(400, "邀请已撤销");
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BusinessException(400, "邀请已过期");
        Long userId = BaseContext.getCurrentId();
        AcceptResultVO resultVO = new AcceptResultVO();
        // 已是成员（含 owner）→ 幂等直接返回，不消耗次数
        PlanRole existingRole = planService.getRole(invite.getPlanId(), userId);
        if (existingRole != null){
            resultVO.setRole(existingRole.name().toLowerCase());
            resultVO.setPlanId(invite.getPlanId());
            return resultVO;
        }
        // 原子占坑：未满额才 +1，检查与自增合一，杜绝并发超限；+1 后若插入协作者失败会随事务回滚
        if (planInviteMapper.incrementUsedIfAvailable(invite.getId()) == 0) {
            throw new BusinessException(400, "邀请次数已用完");
        }
        PlanCollaborator c = new PlanCollaborator();
        c.setPlanId(invite.getPlanId());
        c.setUserId(userId);
        c.setRole(invite.getRole());
        c.setJoinedAt(LocalDateTime.now());
        planCollaboratorMapper.insert(c);
        // 广播「成员加入」事件（actorId/actorName 由 broadcastEvent 统一取自当前登录用户 = 加入者本人）
        planService.broadcastEvent(invite.getPlanId(), "member_join", "role", invite.getRole());
        resultVO.setRole(invite.getRole());
        resultVO.setPlanId(invite.getPlanId());
        return resultVO;
    }

    @Override
    public void revokeInvite(Long planId, Long inviteId) {
        planService.requireOwner(planId);
        PlanInvite invite = planInviteMapper.selectById(inviteId);
        if (invite == null || !invite.getPlanId().equals(planId))
            throw new BusinessException(404, "邀请不存在");
        invite.setRevoked(true);
        planInviteMapper.updateById(invite);
    }

    @Override
    public PlanInviteVO updateInvite(Long planId, Long inviteId, PlanInviteUpdateDTO dto) {
        planService.requireOwner(planId);
        PlanInvite invite = planInviteMapper.selectById(inviteId);
        if (invite == null || !invite.getPlanId().equals(planId))
            throw new BusinessException(404, "邀请不存在");
        if (Boolean.TRUE.equals(invite.getRevoked()))
            throw new BusinessException(400, "邀请已撤销，不能修改");
        // 次数上限不能小于已使用次数（maxUses=0 表示不限，不校验）
        if (dto.getMaxUses() != null && dto.getMaxUses() > 0 && dto.getMaxUses() < invite.getUsedCount())
            throw new BusinessException(400, "次数上限不能小于已使用次数");
        invite.setRole(dto.getRole());
        invite.setMaxUses(dto.getMaxUses());
        invite.setExpiresAt(dto.getExpiresAt());
        planInviteMapper.updateById(invite);
        PlanInviteVO vo = BeanUtil.copyProperties(invite, PlanInviteVO.class);
        vo.setLink(frontendBaseUrl + "/invite/" + invite.getToken());
        return vo;
    }

    @Override
    public List<PlanInviteVO> listInvites(Long planId) {
        planService.requireOwner(planId);
        List<PlanInviteVO> list = planInviteMapper.selectList(
                        new LambdaQueryWrapper<PlanInvite>().eq(PlanInvite::getPlanId, planId))
                .stream().map(i -> {
                    PlanInviteVO vo = BeanUtil.copyProperties(i, PlanInviteVO.class);
                    vo.setLink(frontendBaseUrl + "/invite/" + i.getToken());
                    return vo;
                }).toList();
        return list;
    }
}
